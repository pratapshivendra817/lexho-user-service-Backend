# Lexho Partner App Backend

# Partner App Current Backend Coverage

This section lists everything currently implemented for the Lexho Partner App backend.

## Implemented Modules

- Runtime management: availability update, dashboard, bank details, service areas, leave, payouts, and notifications.
- Bank workflow: bank details get/update, `PENDING`, `VERIFIED`, `REJECTED`, rejection reason, admin verification.
- Orders: new orders, order detail, accept, reject, start, complete, history, dashboard, timeline.
- Order lifecycle validation: `CREATED/ASSIGNED -> ACCEPTED`, `CREATED/ASSIGNED -> REJECTED`, `ACCEPTED -> STARTED`, `STARTED -> COMPLETED`.
- Order timestamps: `acceptedAt`, `startedAt`, `completedAt`, `rejectedAt`.
- Order audit trail: lifecycle activity history with actor and timestamp.
- Partner requests: list, detail, accept, reject, dev seed, expiry handling.
- Request statuses: `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`.
- Dispatch: shift start, shift end, availability, shift history, location update, skills update/get.
- Dispatch config: location foreground/background update intervals.
- Auto offline: GPS disabled, stale location, shift ended, terminal tracking state.
- FCM device tokens: register/delete tokens, multiple devices per partner.
- Notifications: list, mark read, unread count, provider compatibility aliases.
- Earnings: today, weekly, monthly, available balance, pending settlement, lifetime earnings.
- Payouts and withdrawals: payout list/detail, withdrawal request/history/balance.
- Performance: acceptance rate, completion rate, cancellation rate, average rating, on-time arrival rate.
- Ratings: average rating, total reviews, paginated review list.
- Leave management: create leave, list leaves, mark partner offline on leave.
- Service areas: get/update preferred pincodes and cities.
- Penalty system: admin warning, suspension, blocked status and history.
- Upload validation: max file size, allowed image types, corrupted image detection.
- Rate limiting: create order, retry payment, payment verify, partner accept/reject, location updates.
- Standard pagination: `content`, `page`, `size`, `totalElements`, `totalPages`, `last`.
- Smart dispatch scoring: distance, pincode, active jobs, rating, online eligibility.
- Redis fallback: location/availability write paths avoid crashing where possible if Redis is down.
- Swagger/OpenAPI: new controller routes are annotated where controller OpenAPI annotations are used.
- Database migration: `V80__partner_app_production_features.sql` adds production partner fields/tables.

## Primary Partner APIs

```text
PUT   /api/v1/partner/availability
GET   /api/v1/partner/dashboard/stats
GET   /api/v1/partner/banners
GET   /api/v1/orders/new
GET   /api/v1/orders/{id}
POST  /api/v1/orders/{id}/accept
POST  /api/v1/orders/{id}/reject
POST  /api/v1/orders/{id}/start
POST  /api/v1/orders/{id}/complete
GET   /api/v1/orders/{id}/timeline
GET   /api/v1/orders/history
GET   /api/v1/partner/requests
GET   /api/v1/partner/requests/{requestId}
PATCH /api/v1/partner/requests/{requestId}/accept
PATCH /api/v1/partner/requests/{requestId}/reject
GET   /api/v1/partner/dispatch/config
POST  /api/v1/partner/dispatch/shift/start
POST  /api/v1/partner/dispatch/shift/end
PATCH /api/v1/partner/dispatch/availability
PUT   /api/v1/partner/dispatch/availability
POST  /api/v1/partner/dispatch/location
PUT   /api/v1/partner/dispatch/skills
GET   /api/v1/partner/dispatch/skills
POST  /api/v1/partner/device-token
DELETE /api/v1/partner/device-token
GET   /api/v1/partner/notifications
PATCH /api/v1/partner/notifications/{id}/read
GET   /api/v1/partner/notifications/unread-count
GET   /api/v1/partner/earnings/summary
GET   /api/v1/partner/earnings/transactions
GET   /api/v1/partner/payouts
GET   /api/v1/partner/bank-details
PUT   /api/v1/partner/bank-details
POST  /api/v1/partner/withdrawals
GET   /api/v1/partner/withdrawals
GET   /api/v1/partner/withdrawals/balance
GET   /api/v1/partner/performance
GET   /api/v1/partner/ratings
POST  /api/v1/partner/leave
GET   /api/v1/partner/leave
GET   /api/v1/partner/service-areas
PUT   /api/v1/partner/service-areas
```

This README contains the backend contract for the Lexho Partner app. New app screens should use `/api/v1/...`; `/api/...` aliases are kept for backward compatibility.

Local base URL:

```text
http://localhost:8082
```

Auth header:

```http
Authorization: Bearer <partner-jwt>
```

## Standard Response Rules

Protected APIs require a valid partner JWT and derive `partnerId` from the token. Paginated APIs return this contract:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": false
}
```

Rate-limited APIs return HTTP 429:

```json
{
  "code": "RATE_LIMIT_EXCEEDED"
}
```

## Runtime And Bank APIs

```text
PUT  /api/v1/partner/availability
```

## Admin Verification

```text
PATCH /api/v1/admin/partners/{partnerId}/bank/status
PATCH /api/v1/admin/partners/{partnerId}/penalties
GET   /api/v1/admin/partners/{partnerId}/penalties
```

Bank decision:

```json
{
  "status": "VERIFIED",
  "reason": null
}
```

Allowed bank statuses:

```text
PENDING
VERIFIED
REJECTED
```

Penalty request:

```json
{
  "status": "WARNING",
  "reason": "Repeated order rejection"
}
```

Allowed penalty statuses:

```text
WARNING
SUSPENSION
BLOCKED
```

## Dashboard

```text
GET /api/v1/partner/dashboard/stats
GET /api/v1/partner/banners?page=0&size=5
```

Dashboard includes earnings, completed orders, pending orders, online hours, active deliveries, and weekly earnings.

## Orders

```text
GET  /api/v1/orders/new?page=0&size=20&sortBy=createdAt&direction=desc
GET  /api/v1/orders/{id}
POST /api/v1/orders/{id}/accept
POST /api/v1/orders/{id}/reject
POST /api/v1/orders/{id}/start
POST /api/v1/orders/{id}/complete
GET  /api/v1/orders/{id}/timeline
GET  /api/v1/orders/history?page=0&size=20&sortBy=createdAt&direction=desc
GET  /api/v1/orders/dashboard
```

Valid lifecycle transitions:

```text
CREATED/ASSIGNED -> ACCEPTED
CREATED/ASSIGNED -> REJECTED
ACCEPTED         -> STARTED
STARTED          -> COMPLETED
```

Invalid transitions return HTTP 400. Terminal orders cannot be accepted, started, or completed again.

Order response includes lifecycle timestamps:

```text
acceptedAt
startedAt
completedAt
rejectedAt
```

Start order upload is `multipart/form-data`:

```text
beforeImage  required file
```

Complete order upload is `multipart/form-data`:

```text
afterImage  required file
```

Timeline response:

```json
[
  {
    "id": 1,
    "orderId": 97,
    "fromStatus": "ACCEPTED",
    "toStatus": "STARTED",
    "remarks": "Order started",
    "actorType": "PARTNER",
    "actorId": 1,
    "timestamp": "2026-06-09T17:42:00"
  }
]
```

## Partner Requests

```text
PATCH /api/v1/partner/me/status
GET   /api/v1/partner/requests?status=&page=0&size=20
GET   /api/v1/partner/requests/{requestId}
PATCH /api/v1/partner/requests/{requestId}/accept
PATCH /api/v1/partner/requests/{requestId}/reject
PATCH /api/v1/partner/requests/dev/seed
```

Request statuses include:

```text
PENDING
ACCEPTED
REJECTED
EXPIRED
```

Expiry rules:

```text
expiresAt is respected on list, detail, and accept
expired requests cannot be accepted
background scheduler auto-marks stale pending requests as EXPIRED
partners on leave are marked offline and do not receive pending request lists
```

Reject request:

```json
{
  "reason": "Not available"
}
```

Incoming request response contains:

```text
requestId
cartId
customerId
serviceId
serviceType
quantity
scheduledTime
status
createdAt
etaMinutes
expiresAt
address
```

## Dispatch And Location

```text
GET   /api/v1/partner/dispatch/config
POST  /api/v1/partner/dispatch/shift/start
POST  /api/v1/partner/dispatch/shift/end
PATCH /api/v1/partner/dispatch/availability
PUT   /api/v1/partner/dispatch/availability
GET   /api/v1/partner/dispatch/shift/history
GET   /api/v1/partner/dispatch/history
POST  /api/v1/partner/dispatch/location
PUT   /api/v1/partner/dispatch/skills
GET   /api/v1/partner/dispatch/skills
```

Dispatch config response:

```json
{
  "locationUpdateIntervalSeconds": 15,
  "backgroundIntervalSeconds": 30
}
```

Location update request:

```json
{
  "latitude": 28.6139,
  "longitude": 77.209,
  "source": "gps",
  "trackingState": "AVAILABLE",
  "bookingId": 97,
  "customerLatitude": 28.6129,
  "customerLongitude": 77.208,
  "accuracyMeters": 12.5,
  "speedKmh": 18.0,
  "headingDegrees": 90.0,
  "mockLocation": false,
  "rootedDevice": false,
  "gpsEnabled": true
}
```

Auto-offline rules:

```text
gpsEnabled=false in location update
location not updated beyond configured stale threshold
shiftEnd is older than current time
tracking reaches terminal lifecycle state
admin availability lock or inactive partner status prevents availability changes
```

Skills update request:

```json
{
  "skills": ["PLUMBING", "ELECTRICIAN"]
}
```

## FCM Device Tokens

```text
POST   /api/v1/partner/device-token
DELETE /api/v1/partner/device-token
```

Request:

```json
{
  "token": "fcm-token",
  "deviceType": "ANDROID"
}
```

Multiple active devices are supported per partner. Re-registering the same token reactivates it.

## Notifications

```text
GET   /api/v1/partner/notifications?page=0&size=20
PATCH /api/v1/partner/notifications/{id}/read
GET   /api/v1/partner/notifications/unread-count
```

Provider aliases also exist:

```text
GET /api/provider/notifications?page=0&size=20
GET /api/v1/provider/notifications?page=0&size=20
```

## Earnings, Bank, And Payouts

```text
GET /api/v1/partner/earnings/summary
GET /api/v1/partner/earnings/transactions?fromDate=&toDate=&page=0&size=20&sortBy=updatedAt&direction=desc
GET /api/v1/partner/payouts?page=0&size=20
GET /api/v1/partner/payouts/{payoutId}
GET /api/v1/partner/bank-details
PUT /api/v1/partner/bank-details
POST /api/v1/partner/withdrawals
GET /api/v1/partner/withdrawals?page=0&size=20
GET /api/v1/partner/withdrawals/balance
```

Earnings summary includes:

```json
{
  "todayEarnings": 0,
  "weeklyEarnings": 0,
  "monthlyEarnings": 0,
  "availableBalance": 0,
  "pendingSettlement": 0,
  "lifetimeEarnings": 0
}
```

Bank details response includes:

```text
maskedAccountNumber
ifscCode
configured
verificationStatus  PENDING | VERIFIED | REJECTED
rejectionReason
```

## Performance And Ratings

```text
GET /api/v1/partner/performance
GET /api/v1/partner/ratings?page=0&size=20
```

Performance response:

```json
{
  "acceptanceRate": 90.0,
  "completionRate": 85.0,
  "cancellationRate": 5.0,
  "averageRating": 4.7,
  "onTimeArrivalRate": 0.0
}
```

Ratings response:

```json
{
  "averageRating": 4.7,
  "totalReviews": 10,
  "reviews": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1,
    "last": true
  }
}
```

## Leave Management

```text
POST /api/v1/partner/leave
GET  /api/v1/partner/leave
```

Create leave request:

```json
{
  "fromDate": "2026-06-10",
  "toDate": "2026-06-12",
  "reason": "Medical leave"
}
```

Creating leave marks the partner offline for dispatch eligibility.

## Service Area Preferences

```text
GET /api/v1/partner/service-areas
PUT /api/v1/partner/service-areas
```

Update request:

```json
{
  "areas": [
    {
      "pincode": "110001",
      "city": "Delhi"
    }
  ]
}
```

## Smart Dispatch Scoring

Candidate scoring uses `DispatchScoringPolicy` and combines operational signals into one score.

Default formula:

```text
score = distanceWeight * distanceScore
      + pincodeWeight * pincodeScore
      + activeJobsWeight * activeJobsScore
      + ratingWeight * ratingScore
```

Default weights:

```text
distanceWeight   0.45
pincodeWeight    0.25
activeJobsWeight 0.15
ratingWeight     0.15
radiusKm         15
```

Signal rules:

```text
distanceScore decreases as distance approaches radiusKm
pincodeScore is 1.0 when current pincode matches requested pincode
activeJobsScore decreases as active jobs approach max active jobs
ratingScore is averageRating / 5
only online and eligible partners should be dispatched
partners on leave are marked offline
```

## Runtime Notes

Required local services:

```text
MySQL  localhost:3306
Redis  localhost:6379
```

Application port:

```text
8082
```

Redis is used for geo index, nearby discovery, and rate limiting. Redis failures are isolated for availability/location writes where possible; nearby discovery can return empty results if Redis is unavailable.

## Database Migration

New production partner features are added by:

```text
application-module/src/main/resources/db/migration/V80__partner_app_production_features.sql
```

The migration adds order lifecycle timestamps, audit actor fields, partner device tokens, leave records, service areas, penalties, and bank verification status.

## Swagger/OpenAPI

New controller APIs are annotated with Swagger `@Operation` where controllers already use OpenAPI annotations. Regenerate the OpenAPI artifact from the running Spring Boot app if a static client SDK is needed.

## Partner App Integration Checklist

Use these routes first:

```text
GET   /api/v1/partner/dashboard/stats
GET   /api/v1/partner/banners
GET   /api/v1/orders/new
GET   /api/v1/orders/{id}
POST  /api/v1/orders/{id}/accept
POST  /api/v1/orders/{id}/reject
POST  /api/v1/orders/{id}/start
POST  /api/v1/orders/{id}/complete
GET   /api/v1/orders/{id}/timeline
GET   /api/v1/orders/history
GET   /api/v1/partner/requests
PATCH /api/v1/partner/requests/{requestId}/accept
PATCH /api/v1/partner/requests/{requestId}/reject
GET   /api/v1/partner/dispatch/config
PATCH /api/v1/partner/dispatch/availability
POST  /api/v1/partner/dispatch/location
POST  /api/v1/partner/device-token
GET   /api/v1/partner/notifications
PATCH /api/v1/partner/notifications/{id}/read
GET   /api/v1/partner/earnings/summary
GET   /api/v1/partner/earnings/transactions
GET   /api/v1/partner/withdrawals/balance
POST  /api/v1/partner/withdrawals
GET   /api/v1/partner/performance
GET   /api/v1/partner/ratings
POST  /api/v1/partner/leave
GET   /api/v1/partner/service-areas
PUT   /api/v1/partner/service-areas
```

