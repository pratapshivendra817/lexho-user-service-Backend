# Scaling Playbook (1k / 10k / 100k Concurrent Jobs)

## 1k Concurrent Jobs
- Single app cluster (2-3 instances), single Redis primary, single Rabbit node.
- Keep `app.dispatch.geo.partition.enabled=false`.
- Keep `app.geo.events.rabbit.partition.enabled=false`.
- SLO targets:
  - partner ingest p95 < 120ms
  - nearby search p95 < 180ms
  - SSE lag p95 < 2s

## 10k Concurrent Jobs
- Split pods/services:
  - `tracking-ingest`
  - `tracking-read-sse`
  - `dispatch-core`
- Enable Redis GEO partitioning:
  - `app.dispatch.geo.partition.enabled=true`
  - `app.dispatch.geo.partition.precision=4`
- Enable Rabbit routing partitioning:
  - `app.geo.events.rabbit.partition.enabled=true`
  - `app.geo.events.rabbit.partition.count=16`
- Rabbit consumers scale horizontally by queue lag alarms.
- SLO targets:
  - ingest p95 < 180ms
  - nearby p95 < 250ms
  - event publish fail rate < 0.5%

## 100k Concurrent Jobs
- Multi-AZ and region-aware deployment.
- Redis cluster with slot-aware sizing; separate hot/cold keys.
- Rabbit cluster + quorum queues, partition count 32/64 depending on city spread.
- Dedicated SSE gateway tier with sticky/session-affinity strategy.
- Disable non-critical high-frequency events during load-shed mode.
- Add city/geohash shard routing at ingress.
- SLO targets:
  - ingest p95 < 250ms
  - nearby p95 < 350ms
  - SSE disconnect rate < 1%

## Partitioning Strategy (Implemented Hooks)
- Redis GEO:
  - global key: `dispatch:partners:geo`
  - shard key: `dispatch:partners:geo:shard:{geohashPrefix}`
  - member->shard map: `dispatch:partners:geo:member-shard:{partnerId}`
- Rabbit:
  - base routing key: `geo.events`
  - partitioned key: `geo.events.p{n}`
  - topic binding wildcard: `geo.events.#`

## Rollout Order
1. Enable metrics and baseline for 48h.
2. Enable Redis partitioning only.
3. Observe p95 and cache hit/miss.
4. Enable Rabbit partitioning.
5. Scale consumers + validate DLQ trends.

## Alerting Minimum
- Redis GEO op latency
- Rabbit queue depth / DLQ depth
- SSE active connections / disconnect rate
- ingest throttled vs accepted ratio

