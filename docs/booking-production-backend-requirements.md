# Booking Production Backend Requirements

Status: Required before production freeze

Scope: Backend architecture and backend API contracts only.

This document turns the booking flow contract into production-grade backend requirements. The frontend contract in `README.md` remains the client-facing response contract. This document defines the missing backend guarantees required behind that contract.

## 1. Critical Production Blockers

These items must be completed before production launch:

- Define booking, assignment, payment, and refund as separate state machines.
- Enforce valid state transitions at the service layer and database layer.
- Add Razorpay webhook handling with signature verification and replay protection.
- Add durable idempotency for booking creation, payment order creation, payment verification, partner accept, partner reject, start, complete, cancel, and refund APIs.
- Add duplicate payment protection based on booking id, Razorpay order id, Razorpay payment id, and idempotency key.
- Add refund lifecycle and refund reconciliation.
- Add partner assignment lifecycle with timeout, rejection, retry, reassignment, and no-partner terminal handling.
- Add outbox-based domain events for booking, assignment, payment, refund, notification, and payout flows.
- Add dead letter queues and event replay tooling.
- Add strict authorization and ownership checks for customer, partner, and admin APIs.
- Add PII masking and role-based response projections.
- Add operational recovery jobs for stuck bookings, stale assignments, failed payments, and failed refunds.

## 2. State Machines

Booking, assignment, payment, and refund statuses must not be collapsed into one field. Each domain owns its own lifecycle.

### 2.1 Booking Statuses

```text
CREATED
PAYMENT_PENDING
PAYMENT_CONFIRMED
CONFIRMED
ASSIGNMENT_PENDING
PARTNER_ASSIGNED
PARTNER_ON_THE_WAY
PARTNER_ARRIVED
SERVICE_STARTED
SERVICE_IN_PROGRESS
COMPLETED
CANCEL_REQUESTED
CANCELLED
FAILED
EXPIRED
```

### 2.2 Assignment Statuses

```text
NOT_STARTED
SEARCHING
PARTNER_REQUESTED
PARTNER_ACCEPTED
PARTNER_REJECTED
PARTNER_TIMEOUT
REASSIGNING
NO_PARTNER_AVAILABLE
ASSIGNED
CANCELLED
EXPIRED
```

### 2.3 Payment Statuses

```text
NOT_REQUIRED
PENDING
AUTHORIZED
CAPTURED
SUCCESS
FAILED
EXPIRED
CANCELLED
REFUND_PENDING
PARTIALLY_REFUNDED
REFUNDED
REFUND_FAILED
RECONCILIATION_REQUIRED
```

### 2.4 Refund Statuses

```text
REQUESTED
APPROVED
PROCESSING
SUCCESS
FAILED
RETRY_PENDING
REJECTED
```

### 2.5 Partner Request Statuses

```text
PENDING
ACCEPTED
REJECTED
TIMEOUT
EXPIRED
CANCELLED
SUPERSEDED
```

## 3. Transition Rules

The backend must reject invalid transitions with `409 CONFLICT` and a machine-readable error code.

Required transition guarantees:

- A booking cannot move to assignment states before payment is confirmed unless the payment mode explicitly supports post-service collection.
- A cancelled booking cannot move to partner assigned, on the way, arrived, started, in progress, or completed.
- A completed booking cannot be cancelled.
- A refund cannot be created before payment success.
- A partner cannot start service before accepting assignment.
- A partner cannot complete service before service has started.
- A partner cannot accept a request that is expired, cancelled, superseded, or already accepted by another partner.
- A booking with `NO_PARTNER_AVAILABLE` can only move to reassignment retry, cancellation, or scheduled fallback.
- A scheduled booking cannot be assigned before the configured pre-assignment window.

Every transition must persist:

```text
bookingId
previousStatus
newStatus
actorType
actorId
reasonCode
source
traceId
createdAt
metadata
```

## 4. Booking APIs

### 4.1 Create Booking

```text
POST /api/v1/bookings
```

Required headers:

```http
Authorization: Bearer <customer-jwt>
Idempotency-Key: <unique-client-key>
```

Required request fields:

```json
{
  "serviceId": 101,
  "addressId": 55,
  "bookingType": "INSTANT",
  "scheduledAt": null,
  "slotId": null,
  "latitude": 28.4595,
  "longitude": 77.0266,
  "couponCode": null,
  "paymentMode": "ONLINE",
  "note": "Customer note"
}
```

Required response fields:

```json
{
  "bookingId": 20481,
  "bookingStatus": "PAYMENT_PENDING",
  "assignmentStatus": "NOT_STARTED",
  "paymentStatus": "PENDING",
  "refundStatus": null,
  "stateVersion": 1,
  "serverTime": "2026-06-09T12:00:00Z",
  "expiresAt": "2026-06-09T12:15:00Z"
}
```

### 4.2 Booking Details

```text
GET /api/v1/bookings/{bookingId}
```

Required backend response additions:

```json
{
  "stateVersion": 12,
  "etag": "booking-20481-v12",
  "serverTime": "2026-06-09T12:00:00Z",
  "allowedActions": ["CANCEL", "CALL_PARTNER"],
  "nextAction": "WAIT_FOR_PARTNER",
  "bookingStatus": "PARTNER_ON_THE_WAY",
  "assignmentStatus": "ASSIGNED",
  "paymentStatus": "SUCCESS",
  "refundStatus": null
}
```

### 4.3 Timeline

```text
GET /api/v1/bookings/{bookingId}/timeline
```

Each item must include:

```json
{
  "eventId": "evt_123",
  "eventType": "BOOKING_STATUS_CHANGED",
  "previousStatus": "PARTNER_ASSIGNED",
  "newStatus": "PARTNER_ON_THE_WAY",
  "actorType": "PARTNER",
  "actorId": "77",
  "reasonCode": "PARTNER_STARTED_TRAVEL",
  "source": "PARTNER_APP",
  "traceId": "trace-id",
  "createdAt": "2026-06-09T12:00:00Z"
}
```

### 4.4 Cancel Booking

```text
POST /api/v1/bookings/{bookingId}/cancel
```

Required request:

```json
{
  "reasonCode": "CUSTOMER_CHANGED_MIND",
  "comment": "Optional text",
  "requestedBy": "CUSTOMER"
}
```

Cancellation response must include:

```json
{
  "bookingId": 20481,
  "bookingStatus": "CANCELLED",
  "refundRequired": true,
  "refundId": 991,
  "cancellationFee": 0,
  "refundAmount": 499
}
```

## 5. Payment Architecture

### 5.1 Create Payment Order

```text
POST /api/v1/payments/orders
```

Required headers:

```http
Authorization: Bearer <customer-jwt>
Idempotency-Key: <unique-client-key>
```

Duplicate behavior:

- Same idempotency key and same request body returns the original response.
- Same idempotency key and different request body returns `409 IDEMPOTENCY_KEY_CONFLICT`.
- Same booking with active unpaid Razorpay order returns the active order instead of creating a duplicate.
- Same booking with expired Razorpay order creates a new order and marks the old one `EXPIRED`.

### 5.2 Verify Payment

```text
POST /api/v1/payments/verify
```

Required validation:

- Verify Razorpay signature.
- Verify Razorpay order belongs to the booking.
- Verify payment amount equals payable amount.
- Verify currency.
- Reject duplicate payment ids.
- Persist raw gateway response in a restricted audit table.

### 5.3 Razorpay Webhook

```text
POST /api/v1/payments/webhooks/razorpay
```

Required behavior:

- Verify `X-Razorpay-Signature`.
- Persist webhook event id before processing.
- Reject already processed event ids idempotently.
- Process events asynchronously through a durable queue.
- Handle out-of-order events by reconciling with Razorpay API.
- Never trust frontend verification as the final source of truth.

Required webhook events:

```text
payment.authorized
payment.captured
payment.failed
order.paid
refund.created
refund.processed
refund.failed
```

### 5.4 Refund APIs

```text
POST /api/v1/payments/{paymentId}/refund
GET  /api/v1/refunds/{refundId}
POST /api/v1/admin/refunds/{refundId}/retry
```

Refunds must support:

- Full refund
- Partial refund
- Cancellation-linked refund
- Admin-initiated refund
- Failed refund retry
- Webhook-driven final status
- Ledger adjustment

## 6. Partner Assignment

### 6.1 Assignment Creation

```text
POST /api/v1/bookings/{bookingId}/assignment/start
```

Assignment engine must use:

- Partner online status
- Partner skills
- Partner service area
- Partner distance
- Partner rating
- Partner active job count
- Partner fraud or availability lock
- Recent rejection history
- Partner cooldown

### 6.2 Partner Request Expiry

Each partner request must include:

```json
{
  "requestId": 1001,
  "bookingId": 20481,
  "partnerId": 77,
  "status": "PENDING",
  "expiresAt": "2026-06-09T12:01:00Z",
  "attemptNumber": 1
}
```

### 6.3 Reassignment Strategy

Required behavior:

- On partner rejection, mark request `REJECTED` and select next candidate.
- On timeout, mark request `TIMEOUT` and select next candidate.
- On partner offline during active assignment, move booking to `REASSIGNING` if service has not started.
- On max attempts exhausted, move assignment to `NO_PARTNER_AVAILABLE`.
- On no partner available, expose backend action options: retry later, scheduled fallback, or cancel with refund.

### 6.4 Concurrency Control

Partner accept must be atomic:

- Lock booking row or use optimistic `stateVersion`.
- Lock partner request row.
- Accept only if request status is `PENDING`.
- Supersede all other pending requests for the same booking.
- Return `409 REQUEST_ALREADY_HANDLED` if already accepted, rejected, timed out, or superseded.

## 7. Reliability Architecture

### 7.1 Domain Events

Required event names:

```text
booking.created
booking.confirmed
booking.cancelled
booking.completed
booking.expired
assignment.started
assignment.partner_requested
assignment.partner_accepted
assignment.partner_rejected
assignment.partner_timeout
assignment.no_partner_available
payment.order_created
payment.success
payment.failed
payment.expired
refund.requested
refund.success
refund.failed
notification.requested
notification.sent
notification.failed
```

### 7.2 Outbox Pattern

Every transaction that changes booking, payment, refund, or assignment state must write a domain event into an outbox table in the same DB transaction.

Outbox fields:

```text
eventId
aggregateType
aggregateId
eventType
payload
status
retryCount
nextRetryAt
createdAt
publishedAt
traceId
```

### 7.3 Retry And DLQ

Required policies:

- Retry transient failures with exponential backoff.
- Move poison events to DLQ after max retries.
- Provide admin replay endpoint for DLQ events.
- Preserve original event id on replay.
- Alert when DLQ count is greater than zero.

### 7.4 Circuit Breakers

Circuit breakers are required for:

```text
Razorpay
Redis
FCM
SMS provider
Email provider
Reverse geocoding
Map provider
Object storage
```

Each integration must define:

```text
connectTimeoutMs
readTimeoutMs
maxRetries
retryBackoffMs
circuitOpenDurationMs
fallbackBehavior
```

## 8. Security Requirements

### 8.1 JWT Validation

JWT validation must enforce:

- Signature validation
- Issuer validation
- Audience validation
- Expiry validation
- Not-before validation
- Token version or revocation support
- Role and scope checks

### 8.2 Authorization Rules

Required ownership checks:

- Customer can read only own bookings.
- Partner can read only assigned bookings or active partner requests.
- Partner cannot read another partner's requests.
- Admin APIs require admin role and audit reason.
- Payment APIs require booking ownership.
- Refund APIs require admin role except system-generated refunds.

### 8.3 PII Protection

PII fields must be projected by role:

```text
customerName
customerPhone
partnerName
partnerPhone
fullAddress
latitude
longitude
paymentReference
documentUrls
bankDetails
```

Rules:

- Mask phone numbers unless call action is allowed.
- Hide exact customer coordinates until partner is assigned.
- Hide partner coordinates unless tracking is active.
- Never expose document URLs without signed short-lived URLs.
- Never expose bank account numbers in full.

### 8.4 OWASP Controls

Required controls:

- Request validation on all DTOs.
- File upload type, size, and malware checks.
- Rate limiting on auth, booking, payment, partner action, and location APIs.
- CSRF protection where browser cookies are used.
- CORS allowlist.
- Security headers.
- SQL injection protection through parameterized queries only.
- SSRF protection for external URL handling.
- Secrets managed outside source code.

## 9. Observability Requirements

### 9.1 Structured Logging

Every backend log related to booking flow must include:

```text
traceId
requestId
bookingId
userId
partnerId
paymentId
refundId
assignmentId
eventId
api
status
durationMs
errorCode
```

### 9.2 Audit Logs

Audit logs are required for:

- Booking created
- Booking status changed
- Assignment requested
- Partner accepted
- Partner rejected
- Partner timed out
- Booking cancelled
- Payment verified
- Payment webhook processed
- Refund requested
- Refund processed
- Admin override
- Partner payout or withdrawal action

### 9.3 Metrics

Required metrics:

```text
booking_create_count
booking_create_failure_count
booking_state_transition_count
invalid_transition_count
payment_success_count
payment_failure_count
payment_webhook_lag_seconds
refund_success_count
refund_failure_count
assignment_success_count
assignment_timeout_count
assignment_no_partner_count
partner_accept_latency_seconds
api_latency_seconds
api_error_count
outbox_pending_count
dlq_event_count
redis_error_count
razorpay_error_count
```

### 9.4 Tracing

Distributed tracing must cover:

- Booking create
- Payment order create
- Razorpay verify
- Razorpay webhook processing
- Assignment candidate search
- Partner request send
- Partner accept/reject
- Notification send
- Refund processing

## 10. Scalability Requirements

### 10.1 Database

Required indexes:

```text
bookings(user_id, created_at)
bookings(status, created_at)
bookings(booking_type, scheduled_at)
booking_assignments(booking_id)
partner_requests(partner_id, status, expires_at)
partner_requests(booking_id, status)
payments(booking_id)
payments(razorpay_order_id)
payments(razorpay_payment_id)
refunds(payment_id)
outbox_events(status, next_retry_at)
audit_logs(booking_id, created_at)
```

### 10.2 Redis

Redis must be treated as non-authoritative for booking state.

Allowed Redis usage:

- Partner geo index
- Rate limiting
- Short-lived cache
- Distributed lock with DB verification
- SSE/WebSocket fanout support

Redis requirements:

- Define key schema.
- Define TTLs.
- Define memory eviction policy.
- Define rebuild process from database.
- Define fallback behavior when Redis is unavailable.

### 10.3 Polling And Push

Backend should reduce polling pressure using:

- ETag or `If-None-Match` on booking details.
- `updatedSince` query support.
- SSE for active booking status updates.
- WebSocket only when bidirectional communication is required.
- Polling fallback when SSE/WebSocket is unavailable.

## 11. Production Readiness

### 11.1 Disaster Recovery

Required:

- Automated MySQL backups.
- Backup restore test process.
- RPO and RTO documented.
- Redis warmup or rebuild job.
- Object storage backup policy.
- Payment reconciliation after restore.

### 11.2 Monitoring And Alerting

Alerts required:

- Payment webhook failures.
- Payment reconciliation mismatch.
- DLQ event count greater than zero.
- Assignment no-partner rate above threshold.
- Assignment timeout rate above threshold.
- Booking stuck in intermediate state.
- Redis unavailable.
- Razorpay unavailable.
- API 5xx spike.
- P95 or P99 latency breach.
- Database connection pool exhaustion.

### 11.3 Rollback Strategy

Required:

- Backward-compatible API changes only within `/api/v1`.
- Feature flags for new assignment/payment flows.
- Database migrations must be backward compatible.
- No destructive migration without backup and rollback plan.
- Contract tests must run before release.
- Smoke tests must verify booking create, payment, assignment, cancellation, and refund.

## 12. Required Error Codes

```text
BOOKING_NOT_FOUND
BOOKING_NOT_OWNED
BOOKING_ALREADY_CANCELLED
BOOKING_ALREADY_COMPLETED
BOOKING_EXPIRED
INVALID_BOOKING_TRANSITION
INVALID_ASSIGNMENT_TRANSITION
INVALID_PAYMENT_TRANSITION
INVALID_REFUND_TRANSITION
PAYMENT_ORDER_ACTIVE
PAYMENT_ORDER_EXPIRED
PAYMENT_SIGNATURE_INVALID
PAYMENT_AMOUNT_MISMATCH
PAYMENT_DUPLICATE
PAYMENT_RECONCILIATION_REQUIRED
REFUND_NOT_ALLOWED
REFUND_ALREADY_EXISTS
REFUND_FAILED
PARTNER_REQUEST_NOT_FOUND
PARTNER_REQUEST_EXPIRED
PARTNER_REQUEST_ALREADY_HANDLED
PARTNER_UNAVAILABLE
NO_PARTNER_AVAILABLE
IDEMPOTENCY_KEY_REQUIRED
IDEMPOTENCY_KEY_CONFLICT
RATE_LIMIT_EXCEEDED
UNAUTHORIZED
FORBIDDEN
VALIDATION_FAILED
EXTERNAL_SERVICE_UNAVAILABLE
INTERNAL_ERROR
```

## 13. Implementation Priority

Phase 1 production blockers:

- State machines and transition validation
- Payment webhook and reconciliation
- Idempotency framework
- Partner assignment timeout and reassignment
- Cancellation and refund lifecycle
- Authorization and PII projection

Phase 2 reliability:

- Outbox events
- DLQ and replay
- Circuit breakers
- Structured metrics and tracing
- Stuck booking recovery jobs

Phase 3 scale:

- Redis key strategy
- Database index hardening
- ETag or updated-since polling optimization
- SSE active booking events
- Disaster recovery runbooks

## 14. Rate Limiting Contract

Sensitive APIs must use Redis-backed distributed rate limiting. Redis keys must include the API bucket and the configured identity scope. The limiter must work consistently across all application instances.

When a limit is exceeded, the backend must return:

```text
HTTP 429 TOO_MANY_REQUESTS
```

Required error code:

```text
RATE_LIMIT_EXCEEDED
```

The backend must include a `Retry-After` response header when the remaining wait time is known.

Required response headers:

```http
Retry-After: 60
Content-Type: application/json
```

Required error response:

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests",
  "retryAfterSeconds": 60
}
```

Required limits:

| API | Limit |
| --- | --- |
| `POST /otp/send` | 5 requests per 15 minutes per mobile |
| `POST /otp/verify` | 10 requests per 15 minutes per mobile |
| `POST /api/admin/login` | 5 requests per 15 minutes per email |
| `POST /api/payments/create-order` | 10 requests per minute per user |
| `POST /api/payments/retry` | 5 requests per minute per booking |
| `PATCH /api/v1/partner/requests/{id}/accept` | 20 requests per minute per partner |
| `PATCH /api/v1/partner/requests/{id}/reject` | 20 requests per minute per partner |

Required rate limit key schema:

```text
rl:{bucket}:{identity}
```

Required buckets:

```text
otp_send
otp_verify
admin_login
payment_create_order
payment_retry
partner_request_accept
partner_request_reject
```

Identity rules:

- OTP send and verify must use normalized mobile number.
- Admin login must use normalized email address.
- Payment create order must use authenticated user id.
- Payment retry must use booking id.
- Partner accept and reject must use authenticated partner id.

Rate limit state must not be stored only in JVM memory in production. In-memory fallback is allowed only for local development or explicitly degraded non-production mode.

## 15. API Versioning Policy

Current public API version:

```text
v1
```

Frontend integrations should use `/api/v1` routes for new screens and new API integrations.

Examples:

```text
/api/v1/bookings
/api/v2/bookings
```

Versioning rules:

- Breaking API changes must be released as `v2` or a later major version.
- Backward compatibility must be maintained for at least 6 months after a replacement API is released.
- Deprecation announcements must be documented in the backend contract and release notes.
- Legacy routes may remain available during migration.
- Legacy routes must not silently change response semantics.
- New frontend work should not depend on unversioned routes unless the route is explicitly marked as a compatibility alias.

Deprecation notices must include:

```text
deprecatedRoute
replacementRoute
deprecationDate
removalNotBefore
behaviorChangeSummary
owner
```

Example deprecation notice:

```json
{
  "deprecatedRoute": "/api/bookings",
  "replacementRoute": "/api/v1/bookings",
  "deprecationDate": "2026-06-09",
  "removalNotBefore": "2026-12-09",
  "behaviorChangeSummary": "Use versioned booking contract fields",
  "owner": "backend-platform"
}
```

## 16. Security Headers Contract

All production traffic must be served through HTTPS. Plain HTTP must be redirected to HTTPS at the edge or load balancer.

Mandatory response headers:

```http
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

Security header requirements:

- `X-Frame-Options` must prevent clickjacking.
- `X-Content-Type-Options` must prevent MIME sniffing.
- `Referrer-Policy` must avoid leaking sensitive full URLs.
- `Content-Security-Policy` must restrict executable and embeddable content.
- `Strict-Transport-Security` must be enabled only on HTTPS production domains.

Spring Security expectations:

- Security headers must be configured centrally in the Spring Security filter chain.
- HSTS must be enabled for production profiles.
- CSP must be reviewed when frontend asset domains change.
- CORS allowlist must be explicit and environment-specific.
- Swagger and internal endpoints must not be publicly exposed in production unless explicitly approved.

## 17. File Upload Security Rules

File upload APIs must validate files before persistence and before public URLs are returned.

Allowed file extensions:

```text
jpg
jpeg
png
pdf
```

Maximum file size:

```text
5 MB per file
```

Validation requirements:

- Validate MIME type.
- Validate file extension.
- Sanitize original filename.
- Reject path traversal attempts.
- Reject executable files.
- Scan files for malware or viruses before persistence.
- Store files using generated object keys instead of user-provided names.
- Do not expose local filesystem paths in API responses.

Required error codes:

```text
FILE_TOO_LARGE
INVALID_FILE_TYPE
FILE_SCAN_FAILED
```

Example file-too-large response:

```json
{
  "code": "FILE_TOO_LARGE",
  "message": "File size must be 5 MB or less",
  "timestamp": "2026-06-09T10:30:00Z",
  "path": "/api/admin/upload/image"
}
```

Example invalid-file-type response:

```json
{
  "code": "INVALID_FILE_TYPE",
  "message": "Only jpg, jpeg, png, and pdf files are allowed",
  "timestamp": "2026-06-09T10:30:00Z",
  "path": "/api/admin/upload/image"
}
```

Example scan-failed response:

```json
{
  "code": "FILE_SCAN_FAILED",
  "message": "File could not be accepted",
  "timestamp": "2026-06-09T10:30:00Z",
  "path": "/api/admin/upload/image"
}
```

## 18. Service Level Objectives

Production APIs must be monitored against the following SLO targets.

Target response times:

| API | Target |
| --- | --- |
| Booking Details API | < 500 ms |
| Booking Create API | < 1 sec |
| Payment Verify API | < 1 sec |
| Razorpay Webhook Processing | < 5 sec |
| OTP Verify API | < 500 ms |

Availability target:

```text
99.9% uptime
```

Monitoring sources:

```text
Prometheus
Actuator Metrics
Grafana dashboards
```

Required dashboard views:

- API latency by route.
- API error rate by route.
- Rate limit rejections.
- Payment verification latency.
- Webhook processing latency and failures.
- OTP verify latency and failures.
- Booking create success and failure rates.
- Database connection pool usage.
- Redis availability and command latency.

SLO breaches must create operational alerts for the backend owner.

## 19. Notification Delivery Contract

Notification delivery must support multiple channels with durable status tracking.

Supported channels:

```text
PUSH
IN_APP
SMS
WHATSAPP
```

`WHATSAPP` is a future channel and must not be assumed active until provider configuration is enabled.

Notification statuses:

```text
QUEUED
SENT
DELIVERED
FAILED
READ
```

Notification requirements:

- Persist notification request and delivery status.
- Track delivery status changes.
- Retry transient delivery failures.
- Audit notification creation and delivery attempts.
- Validate device tokens before push delivery.
- Support FCM integration for push notifications.
- Do not block critical booking or payment transactions on notification provider latency.
- Preserve notification id across retries.

Required notification delivery payload:

```json
{
  "notificationId": "N123",
  "channel": "PUSH",
  "status": "DELIVERED",
  "sentAt": "2026-06-09T10:00:00Z",
  "deliveredAt": "2026-06-09T10:00:03Z"
}
```

Required failure payload:

```json
{
  "notificationId": "N123",
  "channel": "PUSH",
  "status": "FAILED",
  "failureCode": "FCM_UNAVAILABLE",
  "retryable": true,
  "failedAt": "2026-06-09T10:00:03Z"
}
```

Notification audit fields:

```text
notificationId
recipientId
recipientRole
channel
templateCode
status
attemptCount
providerMessageId
failureCode
createdAt
sentAt
deliveredAt
readAt
traceId
```

## 20. Customer Review & Rating Contract

Customer review APIs are part of the public v1 customer contract and must use authenticated customer identity from JWT.

### Customer Submit Review

```http
POST /api/v1/customer/reviews
```

Request:

```json
{
  "bookingId": 20481,
  "rating": 5,
  "reviewText": "Excellent service"
}
```

Rules:

- Booking must belong to the logged-in customer.
- Booking must be completed. For order-backed bookings, `SERVICE_COMPLETED`, `PAYMENT_COMPLETED`, and `DELIVERED` are exposed as `COMPLETED`.
- Only one active review is allowed per booking.
- `rating` is mandatory and must be between `1` and `5`.
- `reviewText` is optional and must be at most `500` characters.
- Newly submitted reviews are created with `PENDING` status.

Response:

```json
{
  "success": true,
  "data": {
    "reviewId": 1001,
    "bookingId": 20481,
    "rating": 5,
    "reviewSubmitted": true
  }
}
```

### Customer Booking Review Lookup

```http
GET /api/v1/customer/reviews/{bookingId}
```

Response:

```json
{
  "success": true,
  "data": {
    "reviewId": 1001,
    "rating": 5,
    "reviewText": "Excellent service",
    "createdAt": "2026-06-10T10:00:00Z"
  }
}
```

### Booking Details Review Metadata

Booking details responses must include review eligibility inside `meta`:

```json
{
  "meta": {
    "canReview": true,
    "reviewSubmitted": false
  }
}
```

Rules:

- Completed booking with no review: `canReview=true`.
- Existing review: `reviewSubmitted=true` and `canReview=false`.
- Non-completed booking: `canReview=false`.

### Admin Review Moderation

```http
GET /api/v1/admin/reviews
POST /api/v1/admin/reviews/{id}/approve
POST /api/v1/admin/reviews/{id}/reject
```

Moderation rules:

- Approving a review changes status to `APPROVED`.
- Rejecting a review changes status to `REJECTED`.
- Approved review changes recalculate partner rating counters.

Partner rating projection:

```json
{
  "averageRating": 4.75,
  "totalReviews": 128
}
```

### Review Audit Events

The following audit actions are mandatory:

```text
REVIEW_CREATED
REVIEW_APPROVED
REVIEW_REJECTED
```

Audit fields must include actor id, actor role, action, entity type `REVIEW`, entity id, old value, new value, and timestamp.

## 21. Backend Readiness Score

Current backend contract readiness:

```text
4 / 10
```

Target before production freeze:

```text
8 / 10 or higher
```

Primary reason for current score: the existing contract is strong as a frontend response contract, but production backend safety still needs explicit state machines, payment webhook architecture, idempotency, reassignment logic, refunds, security projections, observability, and recovery operations.

