# Production Readiness Report

## Webhook Design

Endpoint:

```text
POST /api/payments/webhook/razorpay
```

Backward-compatible alias:

```text
POST /api/payments/webhook
```

Required headers:

```text
X-Razorpay-Event-Id
X-Razorpay-Signature
```

Supported events:

```text
payment.authorized
payment.captured
payment.failed
refund.created
refund.processed
```

Security:

- Webhook body is verified with Razorpay webhook secret.
- Invalid signatures are rejected.
- `eventId` is stored with a unique constraint.
- Duplicate `SUCCESS` events are ignored.
- `PROCESSING` duplicates are ignored to avoid concurrent double-processing.

## Retry Mechanism

Webhook event table:

```text
payment_webhook_events
```

Fields:

```text
eventId
eventType
bookingId
status
receivedAt
processedAt
retryCount
nextRetryAt
rawPayload
failureReason
```

Statuses:

```text
PENDING
PROCESSING
SUCCESS
FAILED
DEAD_LETTER
```

Retry schedule:

```text
1 min
5 min
15 min
30 min
60 min
```

After max retries, event moves to:

```text
DEAD_LETTER
```

Scheduler:

```text
app.payments.webhook.retry-scheduler-delay-ms=60000
```

## Rate Limiting Strategy

Protected APIs:

```text
POST /api/v1/bookings/create      10 requests/minute
POST /api/bookings/create         10 requests/minute
POST /api/payments/create-order   10 requests/minute
POST /api/payments/retry          3 requests/minute
POST /api/auth/login              5 requests/minute
POST /auth/login                  5 requests/minute
POST /api/admin/login             5 requests/minute
POST /api/admin/verify-otp        5 requests/minute
```

Response:

```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests"
}
```

Redis is used first. If Redis is unavailable, the service falls back to local in-memory rate limiting.

## Audit Log Design

Table:

```text
audit_logs
```

Fields:

```text
userId
partnerId
bookingId
action
oldValue
newValue
createdAt
```

Tracked actions:

```text
BOOKING_CREATED
BOOKING_CANCELLED
BOOKING_RESCHEDULED
PAYMENT_SUCCESS
PAYMENT_FAILED
PAYMENT_REFUNDED
SUBSCRIPTION_PAUSED
SUBSCRIPTION_RESUMED
SUBSCRIPTION_CANCELLED
```

Audit logs are append-only at application level. No update/delete API is exposed.

## Monitoring Dashboard Metrics

Track in dashboards:

```text
API Response Time
API Error Rate
Payment Failure Rate
Booking Creation Rate
Partner Assignment Rate
Webhook Failure Count
Webhook Dead Letter Count
Rate Limit Rejection Count
```

Structured request logs:

```json
{
  "traceId": "",
  "bookingId": "",
  "userId": "",
  "api": "",
  "status": "",
  "durationMs": 0
}
```

## Health Check APIs

Endpoints:

```text
GET /health
GET /health/readiness
GET /health/liveness
```

Aliases:

```text
GET /api/health
GET /api/health/readiness
GET /api/health/liveness
```

Readiness checks:

```text
Database
Redis
Payment Gateway configuration
Notification Service bean
```

Example readiness response:

```json
{
  "status": "UP",
  "checkedAt": "2026-06-08T10:55:00Z",
  "checks": {
    "database": "UP",
    "redis": "UP",
    "paymentGateway": "UP",
    "notificationService": "UP"
  }
}
```

## Alerting Rules

Recommended alerts:

```text
Payment failures > 10%
API error rate > 5%
Database unavailable
Redis unavailable
Webhook failures > 0 for 15 minutes
Webhook DEAD_LETTER count > 0
Readiness status DOWN
```

## Production Readiness Status

Implemented:

- Razorpay webhook signature verification
- Idempotent webhook event table
- Duplicate webhook protection
- Webhook retry scheduler
- Dead-letter state
- Rate limiting for public sensitive APIs
- Immutable audit log table and service
- Payment, booking, subscription audit events
- Structured request logs
- Health/readiness/liveness endpoints
- Production readiness documentation
