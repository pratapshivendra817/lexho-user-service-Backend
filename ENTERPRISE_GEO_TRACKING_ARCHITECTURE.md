# Enterprise Geo/Tracking Architecture (Service Marketplace)

## Scope
Production-grade geo and tracking system for:
- Instant bookings
- Scheduled bookings

Stack:
- Java 17, Spring Boot
- Redis GEO + geohash
- MySQL spatial
- RabbitMQ
- SSE live tracking

---

## 1) Core Tracking Modes

- `LIVE`: continuous live tracking for active travel.
- `PRE_ARRIVAL_ONLY`: enable only shortly before arrival window.
- `CHECKIN_ONLY`: no continuous stream; only check-in/check-out + periodic heartbeat.
- `IDLE`: low-frequency location + presence.
- `OFFLINE`: no tracking.

Mode decision input:
- bookingType (INSTANT / SCHEDULED)
- lifecycle state
- app foreground/background
- active job count

---

## 2) Booking-Type Workflows

### A. Instant Booking
Use case: Uber/Swiggy-like immediate dispatch.

1. Booking created -> `ASSIGNED`
2. Partner accepted -> `EN_ROUTE` + mode `LIVE`
3. Nearby discovery via Redis GEO
4. SSE to customer map with adaptive push
5. Arrival detection -> `ARRIVED`
6. `JOB_STARTED` / `JOB_IN_PROGRESS` -> downgrade to heartbeat
7. `JOB_COMPLETED` -> mode `OFFLINE` (tracking stop)

Optimizations:
- movement threshold before DB/GEO write
- geohash-change-triggered updates
- event suppression for duplicates

### B. Scheduled Booking
Use case: appointment at future slot.

1. Booking created days/hours before
2. Keep partner in `IDLE` (no customer live stream)
3. Tracking activation at `T - preArrivalWindow` (e.g. 20–30 min)
4. dispatch confirm -> `PRE_ARRIVAL_ONLY`
5. after en-route start -> temporary `LIVE`
6. arrival -> downgrade

Cost strategy:
- delayed activation
- avoid long-running SSE before dispatch window
- avoid early ETA recalculations

## 3) Lifecycle State Machine

States:
- `ASSIGNED`
- `EN_ROUTE`
- `ARRIVED`
- `JOB_STARTED`
- `JOB_IN_PROGRESS`
- `JOB_COMPLETED`
- `CANCELLED`
- `OFFLINE`

Textual flow:

`ASSIGNED -> EN_ROUTE -> ARRIVED -> JOB_STARTED -> JOB_IN_PROGRESS -> JOB_COMPLETED -> OFFLINE`

Side paths:
- `ASSIGNED -> CANCELLED -> OFFLINE`
- `EN_ROUTE -> CANCELLED -> OFFLINE`
- any state timeout/disconnect -> `OFFLINE`

---

## 4) Smart Arrival Detection

Arrival is true when any of:
1. distance(customer, partner) <= `arrivalRadiusMeters` (e.g. 50–80m)
2. partner geohash prefix == customer geohash prefix (precision 6/7)
3. low speed + close distance sustained for N seconds

Validation:
- customer coordinates must be verified address point
- reject impossible jumps before arrival transition

---

## 5) Adaptive Intervals (Recommended)

### Ingest (partner -> backend)
- EN_ROUTE / moving: 8–10s
- IDLE moving: 10–20s
- IDLE stable: 20–30s
- JOB_IN_PROGRESS (heartbeat mode): 30–60s
- background app: 30–90s

### SSE push (backend -> customer/admin UI)
- LIVE EN_ROUTE: 5–10s
- PRE_ARRIVAL_ONLY: 10–15s
- ARRIVED/JOB_STARTED: 10–20s
- JOB_IN_PROGRESS: heartbeat-only status, 15–30s

### Heartbeat
- active state: 30s
- idle state: 60s
- stale threshold: 90–180s depending on mode

---

## 6) Cost Optimization Strategy

### Reduce SSE
- only create SSE for active viewers
- auto close stream when job no longer `EN_ROUTE`
- delta payload only (not full object)

### Reduce Redis GEO writes
- write only if moved >= threshold OR geohash changed OR heartbeat due
- avoid write on duplicate rounded coordinates

### Reduce DB writes
- hot state in Redis
- DB writes on significant changes / periodic flush
- coalesce frequent updates

### Reduce Rabbit events
- publish only significant state/location transitions
- dedupe key: `partnerId + geohash + timeBucket`
- route by partition key for balanced consumers

### Reduce bandwidth
- compress SSE payloads
- keep map marker update minimal
- no high-frequency updates in non-live modes

### Reduce map provider API usage
- reverse geocode cache by rounded coords
- ETA cache with short TTL during EN_ROUTE
- no ETA recompute in CHECKIN_ONLY mode

---

## 7) Geohash Optimization

- store geohash(7) in location row for locality grouping
- use geohash(4/5) for shard routing (city/zone partition)
- use prefix match for cheap neighborhood checks
- use geohash change as event trigger filter

---

## 8) Stale Detection & Recovery

Stale logic:
- if no heartbeat beyond threshold => mark `STALE`
- stale partners excluded from assignment/search
- if stale crosses max threshold => `OFFLINE`

Recovery:
- reconnect endpoint asks server for last lifecycle state
- resume mode from Redis state
- replay last known marker on SSE reconnect

---

## 9) Redis GEO Strategy (Scalable)

Keys:
- `dispatch:partners:geo` (global)
- `dispatch:partners:geo:shard:{gh4}` (partition)
- `tracking:state:{partnerId}`
- `partner:lastSeen:{partnerId}`

Read path:
1. search local shard
2. fallback global if low results

Write path:
1. update global
2. update shard + member-shard mapping

---

## 10) RabbitMQ Event Flow

Events:
- `PARTNER_ONLINE`
- `PARTNER_MOVED`
- `ARRIVED_DETECTED`
- `TRACKING_MODE_CHANGED`
- `JOB_CHECKIN`
- `JOB_CHECKOUT`
- `PARTNER_OFFLINE`

Flow:
1. Producer publishes with partitioned routing key (`geo.events.pN`)
2. Topic exchange routes to queue groups
3. Consumers process idempotently
4. failure -> DLQ
5. admin replay endpoint requeues controlled batch

---

## 11) Database Design Recommendations

Tables:
- `current_partner_locations` (hot snapshot, geo point, geohash, updatedAt)
- `tracking_sessions` (bookingId, partnerId, mode, start/end, lifecycle state)
- `tracking_heartbeats` (optional aggregated audit)
- `location_audit_logs` (risk and compliance trails)

Indexes:
- spatial index on geo point
- `(partner_id, updated_at)`
- `(booking_id, lifecycle_state)`

Retention:
- raw high-frequency logs short TTL
- aggregated summaries long retention

---

## 12) API Design Recommendations

Partner APIs:
- `POST /api/v1/partner/dispatch/location` (includes lifecycle state + customer context)
- `POST /api/v1/partner/dispatch/checkin`
- `POST /api/v1/partner/dispatch/checkout`

Customer APIs:
- `GET /api/v1/partners/live/{partnerId}` (SSE)
- `POST /api/v1/partners/nearby`

Admin APIs:
- restricted geofence controls
- DLQ replay
- tracking diagnostics

---

## 13) Frontend Map Update Recommendations

- Only subscribe SSE when tracking mode needs map
- Pause map updates in background tabs
- Smooth marker interpolation client-side
- Show stale badge if last update too old
- Stop stream after `ARRIVED` for check-in-only jobs

---

## 14) Observability (Must-Have)

Metrics:
- location updates accepted vs throttled
- Redis GEO write/read latency
- SSE active connections + drop rate
- Rabbit publish rate + DLQ depth
- stale/offline transitions
- ETA compute latency and cache hit ratio

SLOs:
- ingest p95
- nearby search p95
- SSE lag p95
- event loss (should be near zero)

---

## 15) Scale Architecture

### 1k concurrent jobs
- monolith + Redis + Rabbit single cluster
- partitioning optional off

### 10k concurrent jobs
- split ingest, read/SSE, dispatch workers
- enable Redis GEO shard keys
- enable Rabbit partition routing
- horizontal consumer scaling

### 100k concurrent jobs
- region/city sharding
- dedicated tracking gateway layer
- Redis cluster, Rabbit quorum queues
- load-shedding rules for non-critical streams

---

## 16) Real-world Style Mapping

- Uber-style: strong EN_ROUTE live mode + aggressive stale detection
- Urban Company-style: scheduled defaults to pre-arrival/check-in model
- Swiggy/Zomato-style: map-heavy only in delivery-travel phase, downshift post-arrival

---

## 17) Spring Boot Implementation Strategy

1. Introduce tracking policy service (state -> interval/mode).
2. Apply policy guard in ingest endpoint before DB/Redis writes.
3. Persist lifecycle state in Redis for reconnect continuity.
4. SSE controller resolves interval from lifecycle state.
5. Rabbit publisher uses partition routing service.
6. Add idempotency keys and duplicate suppression.
7. Add metric tags by booking type + tracking mode.

---

## 18) Startup-Friendly Cost Plan

Phase 1:
- adaptive intervals + threshold writes + SSE gating

Phase 2:
- Redis shard keys + Rabbit partition keys

Phase 3:
- analytics aggregation + full SLO alerting

This phased approach keeps infra spend controlled while improving reliability and scale readiness.
