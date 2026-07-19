# Lexho Booking Flow Contract

Status: API Freeze Candidate

OpenAPI freeze spec: [docs/booking-flow-openapi.yaml](docs/booking-flow-openapi.yaml)

Production readiness report: [docs/production-readiness-report.md](docs/production-readiness-report.md)

Production backend requirements: [docs/booking-production-backend-requirements.md](docs/booking-production-backend-requirements.md)

Backend is the single source of truth for:

- Tracking screen selection
- Map visibility
- Polling interval
- Booking actions
- Subscription actions
- Booking timeline
- Payment retry behavior

## Production Reliability Additions

### Razorpay Webhook

Endpoint:

```http
POST /api/v1/webhooks/razorpay
X-Razorpay-Event-Id: evt_123
X-Razorpay-Signature: <hmac-sha256>
Content-Type: application/json
```

Supported events:

- `payment.captured`
- `payment.failed`
- `refund.processed`

Example:

```bash
curl -X POST http://localhost:8082/api/v1/webhooks/razorpay \
  -H "X-Razorpay-Event-Id: evt_123" \
  -H "X-Razorpay-Signature: <signature>" \
  -H "Content-Type: application/json" \
  --data @razorpay-webhook.json
```

Behavior:

- Verifies `X-Razorpay-Signature` using `app.payment.razorpay.webhook-secret`.
- Stores every webhook in `payment_webhook_events`.
- Uses idempotency key `event:<eventId>` and, for payment captured/failed, `eventType:paymentId`.
- Duplicate successful or in-flight events return `200 OK` and are ignored safely.
- Processing failures are persisted with retry metadata and return `202 Accepted`.
- Invalid signatures return `401 Unauthorized`.

### Error Contract

Global non-admin API exceptions use:

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "Order not found",
  "timestamp": "2026-06-09T10:30:00Z",
  "path": "/api/v1/orders/1"
}
```

Covered categories include resource-not-found, validation, access denied, illegal state, rate limiting, request body validation, and unexpected exceptions.

### Observability

Actuator endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`
- `/actuator/metrics`

Custom counters:

- `orders_created`
- `orders_completed`
- `payment_success`
- `payment_failed`
- `partner_acceptance`

Request tracing:

- Incoming `X-Correlation-Id` is propagated.
- `X-Request-Id` is generated when missing.
- Both IDs are added to MDC and response headers.
- Request start/end logs are emitted as structured JSON.

### Audit Log

Migration `V81__production_observability_audit_contract.sql` creates `audit_log`:

- `actor_id`
- `actor_role`
- `action`
- `entity_type`
- `entity_id`
- `old_value`
- `new_value`
- `timestamp`

Audited actions include payment events, KYC/bank reviews, review create/approve/reject events, penalty updates, partner status changes, and order lifecycle transitions.

## Complete App Feature Inventory

This section documents the customer app, partner app, admin app, login system, and shared backend capabilities currently present in this repository.

Base runtime:

- Default backend port: `8082`
- Public base URL examples: `http://localhost:8082`, `http://<LAN-IP>:8082`
- API docs: `/swagger-ui.html`, `/v3/api-docs`
- Health and metrics: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`
- Protected APIs require: `Authorization: Bearer <access_token>`
- Request tracing headers: `X-Request-Id`, `X-Correlation-Id`

### Login And Auth System

Customer and partner OTP auth:

- `POST /otp/send`
- `POST /otp/verify`
- `POST /api/auth/refresh`
- `POST /auth/refresh-token`
- `POST /internal/auth/token`
- `POST /internal/auth/refresh`
- `POST /internal/auth/revoke`

Customer/partner OTP request:

```json
{
  "mobile": "9876543210",
  "role": "CUSTOMER"
}
```

Partner OTP request uses:

```json
{
  "mobile": "9876543210",
  "role": "PARTNER"
}
```

OTP verify response includes:

- `token` / access JWT
- `refreshToken`
- user profile object
- role: `CUSTOMER`, `PARTNER`, or `ADMIN`

Admin login:

- `POST /api/admin/login`
- `POST /api/admin/verify-otp`

Admin login request:

```json
{
  "email": "admin@lexho.com",
  "password": "<admin-password>"
}
```

Admin OTP verify request:

```json
{
  "email": "admin@lexho.com",
  "otp": "123456"
}
```

JWT route protection:

- `/api/customer/**` requires `ROLE_CUSTOMER`
- `/api/v1/customer/**` requires `ROLE_CUSTOMER`
- `/api/partner/**` requires `ROLE_PARTNER`
- `/api/v1/partner/**` requires `ROLE_PARTNER`
- `/api/admin/**` requires `ROLE_ADMIN`
- `/api/v1/admin/**` requires `ROLE_ADMIN`

Public or unauthenticated routes include:

- `/otp/**`
- `/auth/**`
- `/api/otp/**`
- `/api/auth/**`
- `/api/admin/login`
- `/api/admin/verify-otp`
- `/api/payments/webhook`
- `/api/payments/webhook/razorpay`
- `/api/v1/webhooks/razorpay`
- `/health`, `/ping`, `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`
- `/uploads/**`, `/images/**`

### Customer App

Customer app capabilities currently implemented:

- OTP login and refresh-token auth.
- Customer home screen content.
- Home banners and dynamic sections.
- Service catalogue browsing.
- Categories, services, service details, popular services.
- Cart management, add/update/remove cart items.
- Add-on support and add-on pricing through cart/order flows.
- Cart policy/config support.
- Cook family-size pricing support.
- Location and address management.
- Current location save/update.
- Reverse geocoding.
- Serviceability checks.
- Dynamic serviceability and geofence checks.
- Instant and scheduled booking flows.
- Subscription booking flows.
- Subscription quote, pricing, visits, calendar, pause/resume/cancel, skip/reschedule visits.
- Payment options, create Razorpay order, verify payment, retry payment, fetch latest payment status.
- Refund initiation API.
- Customer profile APIs.
- Device token registration for push notifications.
- Customer WebSocket/SSE notification support.
- Live tracking/map metadata in booking responses.

Customer API groups:

| Area | Endpoints / Prefixes |
| --- | --- |
| OTP auth | `POST /otp/send`, `POST /otp/verify`, `POST /api/auth/refresh`, `POST /auth/refresh-token` |
| Home banners | `GET /api/customer/banners`, `GET /api/customer/home/banners`, `GET /api/customer/home/sections` |
| Services/catalogue | `GET /api/customer/services`, `GET /api/customer/services/{id}`, `GET /api/categories`, `GET /api/categories/**` |
| Cart | `/api/customer/cart/**`, fallback cart APIs under customer cart controllers |
| Location/address | `/api/customer/location/**`, `/api/v1/customer/location/**`, `/api/customer/addresses/**` |
| Booking/order | `POST /api/v1/bookings/create`, customer booking and order APIs in `LexhoExperienceController` and `OrderController` |
| Subscription | `/api/subscription/**`, `/api/subscriptions/**` |
| Payments | `/api/payments/options`, `/api/payments/create-order`, `/api/payments/verify`, `/api/payments/retry`, `/api/payments/{bookingId}`, `/api/payments/refund` |
| Profile | customer profile controllers under `/api/customer/profile/**` |
| Notifications | `/api/notifications/devices`, customer notification WebSocket/SSE channels |
| Reviews | `POST /api/v1/customer/reviews`, `GET /api/v1/customer/reviews/{bookingId}` |

Customer app important response contracts:

- Booking responses include `contractVersion`, `statusHistory`, and `meta`.
- Review metadata is exposed as `meta.canReview` and `meta.reviewSubmitted`.
- `meta` is the single source of truth for UI behavior such as map visibility, tracking screen, cancel/reschedule flags, and polling interval.
- Timestamps should be ISO-8601 UTC where exposed in public contracts.

### Partner App

Partner app capabilities currently implemented:

- Partner OTP login and refresh-token auth.
- Partner onboarding.
- Provider/partner document upload.
- Partner profile.
- Bank details.
- Service areas.
- Device token registration.
- Partner availability.
- Shift start/end and shift history.
- Partner skills.
- Partner live location update.
- Dispatch configuration and dispatch history.
- New order/request listing.
- Request accept/reject.
- Partner order accept/reject/start/complete flow.
- Before/after service image upload.
- Partner order timeline/history.
- Dashboard statistics.
- Earnings summary.
- Earnings transaction history.
- Payout list and payout details.
- Withdrawal balance.
- Withdrawal request creation and history.
- Leave management.
- Ratings/reviews.
- Notifications.
- Partner live tracking endpoints.
- Settlement refund reversal integration through payment refund events.

Partner API groups:

| Area | Endpoints / Prefixes |
| --- | --- |
| OTP auth | `POST /otp/send`, `POST /otp/verify`, `POST /api/auth/refresh`, `POST /auth/refresh-token` |
| Onboarding | `POST /api/partner/onboarding`, `POST /api/partner/documents/upload`, `GET /api/partner/documents/{id}` |
| Profile/bank/service area | `/api/partner/profile/**`, partner profile controllers, bank details DTO/service APIs, `/api/partner/service-areas/**` |
| Dispatch/location | `/api/partner/dispatch/**`, `/api/v1/partner/dispatch/**`, `/api/v1/partners/nearby`, `/api/v1/partners/live/{partnerId}` |
| Requests | `/api/partner/requests`, `/api/v1/partner/requests`, accept/reject request routes |
| Orders | `/api/partner/orders/**`, `/api/v1/partner/orders/**`, compatibility routes `/api/partner/dispatch/**` |
| Earnings/payouts | `/api/partner/earnings/**`, `/api/partner/payouts/**`, `/api/partner/withdrawals/**` |
| Leave/ratings | partner leave APIs, `/api/partner/reviews` |
| Notifications | partner notification controllers, `/api/notifications/devices` |

Partner order lifecycle:

- New/assigned order visible to partner.
- Partner accepts or rejects.
- Accepted order can be started.
- Started order can be completed.
- Completion can generate partner payout.
- Refund processing can reverse partner settlement.

### Admin App

Admin app capabilities currently implemented:

- Admin email/password login with OTP verification.
- Admin JWT role protection.
- Dashboard stats and analytics.
- Revenue analytics.
- Service distribution, service summary, top services, service analytics.
- Category management.
- Service management.
- Service detail management.
- Media/image upload.
- Home banner management.
- Popular service management.
- Section and section-item management.
- Add-on management and service/category add-on mapping.
- Cart config management.
- Cook family-size management.
- Cleaning coverage management.
- Cook coverage management.
- Subscription pricing management.
- Subscription management, visits, calendar, partner assignment and replacement.
- Customer management.
- Customer account delete request management.
- Partner/provider onboarding governance.
- Partner KYC document review.
- Partner KYC status review.
- Partner bank verification review.
- Partner availability/risk governance.
- Partner skills governance.
- Partner penalty management.
- Partner settlements and withdrawal admin actions.
- Booking management.
- Booking status/cancel/tracking/payment reconciliation/payout updates.
- Geo city/zone management.
- Serviceable area and serviceability rules.
- Restricted area and scale profile operations.
- Geo event DLQ replay.
- Notification management and bulk notifications.
- Review moderation and analytics.
- Payment admin dashboard, reconciliation, transaction list, refund management.
- User management from `user-service` module.

Admin API groups:

| Area | Endpoints / Prefixes |
| --- | --- |
| Auth | `POST /api/admin/login`, `POST /api/admin/verify-otp` |
| Dashboard | `/api/admin/dashboard/**`, `/api/v1/admin/dashboard/**` |
| Categories/services | `/api/admin/categories`, `/api/admin/services`, `/api/v1/admin/**` service routes |
| Service details | `/api/admin/services/{serviceId}/details/**` |
| Upload/media | `/api/admin/upload/image`, banner upload routes |
| Home banners | `/api/admin/home-banners/**`, `/api/v1/admin/home-banners/**` |
| Popular services | admin popular-service controllers |
| Sections | `/api/admin/sections/**` |
| Add-ons | `/api/admin/addons/**` |
| Cart config | `/api/admin/cart-config` |
| Family/pricing/coverage | `/api/admin/cook-family-sizes/**`, `/api/admin/cleaning-coverage/**`, `/api/admin/cook-coverage/**`, `/api/admin/subscription-pricing/**` |
| Subscriptions | `/api/admin/subscriptions/**`, `/api/v1/admin/subscriptions/**` |
| Customers | `/api/admin/customers/**`, `/api/v1/admin/customers/**` |
| Users | `/api/admin/users/**` |
| Partners | `/api/admin/partners/**`, `/api/v1/admin/partners/**`, admin partner-control routes |
| Booking management | `/api/admin/bookings/**`, `/api/v1/admin/bookings/**`, `/api/v1/admin/booking-management/**` |
| Payments | `/api/admin/payments/**`, admin payment reconciliation/refund routes |
| Geo/serviceability | `/api/admin/geo/**`, `/api/v1/admin/geo/**`, serviceable-area routes |
| Notifications | `/api/admin/notifications/**` |
| Reviews | `/api/admin/reviews/**`, `/api/v1/admin/reviews/**`, `POST /api/v1/admin/reviews/{id}/approve`, `POST /api/v1/admin/reviews/{id}/reject` |

### Payment And Webhook System

Customer payment APIs:

- `GET /api/payments/options?bookingId={bookingId}`
- `POST /api/payments/create-order`
- `POST /api/payments/verify`
- `POST /api/payments/retry`
- `GET /api/payments/{bookingId}`
- `POST /api/payments/refund`

Razorpay webhook APIs:

- `POST /api/v1/webhooks/razorpay`
- `POST /api/payments/webhook/razorpay`
- `POST /api/payments/webhook`

Webhook supported events:

- `payment.captured`
- `payment.failed`
- `refund.processed`

Webhook reliability:

- Signature verification via `X-Razorpay-Signature`.
- Idempotency via event ID and payment ID.
- `payment_webhook_events` audit table.
- Retry metadata: status, retry count, next retry time, failure reason.
- Duplicate events are safely ignored.

### Notification System

Implemented notification capabilities:

- Admin bulk notifications.
- Notification templates and notification types.
- FCM push integration toggle.
- Device token registration.
- In-app notification support.
- Customer booking notification channels.
- Partner/admin notification controllers.
- WebSocket endpoints for notifications.

### Location, Geo, Dispatch, Tracking

Implemented capabilities:

- Customer current location.
- Address save/update/delete.
- Reverse geocoding.
- Serviceability checks.
- Dynamic serviceability rules.
- Pincode/city/zone support.
- Geofence validation.
- Restricted area operations.
- Partner location updates.
- Partner geo indexing.
- Nearby partner discovery.
- Partner tracking lifecycle.
- Live tracking SSE.
- Geo event RabbitMQ configuration and DLQ replay support.

### Subscription System

Implemented capabilities:

- Subscription booking create/quote.
- Subscription pricing.
- Cook pricing.
- Cleaning duration pricing.
- Cook family-size pricing.
- Cook coverage.
- Subscription list/detail.
- Subscription visits and calendar.
- Pause/resume/cancel.
- Visit skip/reschedule.
- Admin assignment/replacement partner.
- Admin visit status control.

### Audit, Observability, Reliability

Implemented production support:

- Global exception handling.
- Standard error response for global exceptions.
- Structured request logging.
- Request ID and correlation ID propagation.
- Actuator health/info/metrics/prometheus.
- Business counters for orders, payments, partner acceptance.
- Audit log table and service.
- Audit events for payment, KYC, bank review, partner status, penalties, order lifecycle.
- Transaction management on critical flows.

### Exhaustive Controller Inventory

The repository currently exposes controllers across these app areas. This inventory is based on controller classes present in the backend source.

Customer-facing controllers:

- `LexhoAuthController`: customer login/OTP auth compatibility flow.
- `LexhoExperienceController`: customer home, booking, payment/refund, experience APIs.
- `LexhoAddressController`: customer address APIs.
- `LexhoUserProfileController`: customer profile APIs.
- `CustomerBannerController`: customer banners.
- `CustomerSectionController`: customer home sections.
- `CustomerServiceController`: customer services/catalogue.
- `CustomerCartFallbackController`: customer cart fallback APIs.
- `CustomerLocationController`: location, address, reverse geocode, serviceability.
- `CustomerProfileController`: customer profile endpoints.
- `SubscriptionController`: customer subscription APIs.
- `CookCoverageController`: public/admin cook coverage APIs.
- `PaymentController`: payment options/create/verify/retry/latest.
- `OrderController`: customer order APIs.
- `PushTokenController`: device-token registration.

Partner-facing controllers:

- `ProviderOnboardingController`: partner onboarding and documents.
- `PartnerController`: partner CRUD/basic partner APIs.
- `LexhoPartnerController`: partner profile, dashboard, earnings, payouts, withdrawals, leave/service-area features.
- `LexhoPartnerOrderController`: partner order lifecycle.
- `LexhoPartnerNotificationController`: partner notifications.
- `PartnerDispatchController`: shift, availability, skills, location, dispatch history/config.
- `PartnerDispatchCompatibilityController`: dispatch/order compatibility APIs.
- `PartnerRequestController`: partner requests, accept/reject, status.
- `PartnerDiscoveryController`: nearby partner discovery.
- `PartnerLiveTrackingController`: live partner tracking SSE.
- `ProviderReviewController`: partner reviews.

Admin-facing controllers:

- `AdminAuthController`: admin login and OTP verify.
- `AdminController`: admin dashboard compatibility endpoint.
- `AdminDashboardController`: dashboard analytics.
- `AdminManagementController`: categories, services, service details, media debug.
- `AdminCategoryController` / admin category routes in management controllers.
- `AdminPopularServiceController`: popular services.
- `AdminSectionController`: homepage sections.
- `AdminAddonController`: add-ons and mappings.
- `AdminBannerPromotionController`: home banners.
- `AdminCartConfigController`: cart config.
- `AdminCookFamilySizeController`: cook family sizes.
- `AdminCleaningCoverageController`: cleaning coverage.
- `CookCoverageController`: admin cook coverage.
- `AdminSubscriptionController`: admin subscription management.
- `AdminSubscriptionPricingController`: subscription pricing.
- `AdminCustomerManagementController`: customers and delete requests.
- `AdminUserManagementController`: users.
- `AdminPartnerControlController`: partner governance, KYC, bank, risk, settlements, withdrawals, penalties.
- `AdminProviderOnboardingController`: provider onboarding admin review.
- `AdminBookingController`: admin booking CRUD/stats.
- `AdminBookingManagementController`: booking operations, tracking, payment reconciliation, payout.
- `AdminPaymentController`: payment admin dashboard/reconciliation/refunds.
- `AdminGeoController`: cities and zones.
- `AdminGeoEventOpsController`: geo event DLQ replay.
- `AdminScaleProfileController`: scale profile operations.
- `AdminServiceabilityOpsController`: serviceability operations.
- `AdminServiceableAreaController`: serviceable area management.
- `AdminRestrictedAreaController`: restricted area operations.
- `AdminNotificationController`: admin notifications.
- `AdminReviewController`: review moderation and stats.
- `AdminSettingsController`: admin settings.

Shared/internal/platform controllers:

- `AuthTokenController`: internal token, refresh, revoke.
- `InternalBookingController`: internal booking summaries.
- `HealthController` and `RootController`: health/ping.
- `StatusController`: status.
- `OpsController`: operational endpoints.
- `SubCategoryController`: subcategory APIs.
- `RazorpayWebhookController`: payment webhook.

### Known Compatibility Notes

- Many APIs expose both legacy `/api/...` and newer `/api/v1/...` paths.
- Admin APIs use `/api/admin/**` and `/api/v1/admin/**`.
- Partner APIs use `/api/partner/**` and `/api/v1/partner/**`.
- Customer APIs use `/api/customer/**`, `/api/v1/customer/**`, and some legacy `/api/subscription/**` routes.
- Do not infer UI behavior from raw booking statuses. Use booking response `meta`.
- Admin frontend should use `/api/admin/login` and `/api/admin/verify-otp`, not generic OTP routes.

Frontend must not infer business rules from status values. Frontend should read UI behavior only from `data.meta`.

## 1. Contract Standards

Every booking response must include:

```json
{
  "contractVersion": 1,
  "statusHistory": [],
  "meta": {
    "trackingScreen": "INSTANT_STATUS",
    "showMap": false,
    "trackingRefreshSeconds": 15,
    "canCancel": true,
    "canReschedule": false,
    "canCallPartner": false,
    "canTrackPartner": false,
    "canRaiseIssue": false
  }
}
```

Timestamp format is ISO-8601 UTC.

Example:

```text
2026-06-08T10:55:00Z
```

Do not expose duplicate UI-control fields outside `meta`.

Do not expose duplicate partner coordinates outside `map.partner`.

## 2. Instant Booking Flow

### Step 1: Create Instant Booking

`POST /api/v1/bookings/create`

Request:

```json
{
  "serviceId": 101,
  "addressId": 55,
  "latitude": 28.4595,
  "longitude": 77.0266,
  "coordinates": {
    "lat": 28.4595,
    "lng": 77.0266
  },
  "bookingType": "INSTANT",
  "note": "Instant booking requested"
}
```

Response:

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "id": 20481,
    "bookingId": 20481,
    "contractVersion": 1,
    "bookingType": "INSTANT",
    "status": "CONFIRMED",
    "assignmentStatus": "PARTNER_ASSIGNED",
    "serviceId": 101,
    "serviceName": "Home Cleaning",
    "createdAt": "2026-06-08T13:00:00Z",
    "promisedStartTime": "2026-06-08T13:30:00Z",
    "slaMinutes": 30,
    "totalAmount": 499,
    "fullAddress": "Sector 45, Gurugram",
    "paymentStatus": "PENDING",
    "retryPaymentAllowed": false,
    "statusHistory": [],
    "meta": {
      "trackingScreen": "INSTANT_STATUS",
      "showMap": false,
      "trackingRefreshSeconds": 15,
      "canCancel": true,
      "canReschedule": false,
      "canCallPartner": false,
      "canTrackPartner": false,
      "canRaiseIssue": false
    }
  }
}
```

### Step 2: Fetch Payment Options

`GET /api/payments/options?bookingId=20481`

Response:

```json
{
  "success": true,
  "data": {
    "options": [
      {
        "id": "upi",
        "mode": "UPI",
        "label": "UPI",
        "description": "Pay using any UPI app"
      },
      {
        "id": "card",
        "mode": "CARD",
        "label": "Card",
        "description": "Credit and debit cards"
      }
    ]
  }
}
```

### Step 3: Create Razorpay Order

`POST /api/payments/create-order`

Header:

```json
{
  "Idempotency-Key": "booking-20481-unique-key"
}
```

Request:

```json
{
  "bookingId": 20481
}
```

Response:

```json
{
  "success": true,
  "data": {
    "bookingId": 20481,
    "keyId": "rzp_test_xxxxx",
    "razorpayOrderId": "order_Nabc123",
    "amountInPaise": 49900,
    "currency": "INR",
    "paymentStatus": "PENDING",
    "retryPaymentAllowed": false
  }
}
```

### Step 4: Verify Payment

`POST /api/payments/verify`

Request:

```json
{
  "razorpayOrderId": "order_Nabc123",
  "razorpayPaymentId": "pay_Nxyz456",
  "razorpaySignature": "generated_signature"
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "bookingId": 20481,
    "paymentStatus": "SUCCESS",
    "status": "SUCCESS",
    "retryPaymentAllowed": false
  }
}
```

Failure response:

```json
{
  "success": false,
  "code": "PAYMENT_FAILED",
  "message": "Payment verification failed",
  "data": {
    "bookingId": 20481,
    "paymentStatus": "FAILED",
    "retryPaymentAllowed": true
  }
}
```

### Step 5: Retry Failed Payment

`POST /api/payments/retry`

Request:

```json
{
  "bookingId": 20481
}
```

Response:

```json
{
  "success": true,
  "data": {
    "bookingId": 20481,
    "keyId": "rzp_test_xxxxx",
    "razorpayOrderId": "order_retry_123",
    "amountInPaise": 49900,
    "currency": "INR",
    "paymentStatus": "PENDING",
    "retryPaymentAllowed": false
  }
}
```

Supported payment statuses:

```text
PENDING
SUCCESS
FAILED
EXPIRED
REFUNDED
```

## 3. Scheduled Booking Flow

### Step 1: Create Scheduled Booking

`POST /api/v1/bookings/create`

Request:

```json
{
  "serviceId": 101,
  "addressId": 55,
  "latitude": 28.4595,
  "longitude": 77.0266,
  "coordinates": {
    "lat": 28.4595,
    "lng": 77.0266
  },
  "bookingType": "SCHEDULED",
  "date": "2026-06-09",
  "slot": "10:00-11:00",
  "note": "Booking from customer app"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "id": 20482,
    "bookingId": 20482,
    "contractVersion": 1,
    "bookingType": "SCHEDULED",
    "status": "CONFIRMED",
    "assignmentStatus": "SCHEDULED",
    "serviceId": 101,
    "serviceName": "Home Cleaning",
    "createdAt": "2026-06-08T13:00:00Z",
    "scheduledAt": "2026-06-09T04:30:00Z",
    "date": "2026-06-09",
    "slot": "10:00-11:00",
    "totalAmount": 499,
    "fullAddress": "Sector 45, Gurugram",
    "paymentStatus": "PENDING",
    "statusHistory": [],
    "meta": {
      "trackingScreen": "SCHEDULED_STATUS",
      "showMap": false,
      "trackingRefreshSeconds": 0,
      "canCancel": true,
      "canReschedule": true,
      "canCallPartner": false,
      "canTrackPartner": false,
      "canRaiseIssue": false
    }
  }
}
```

Payment flow is same as instant booking.

### Reschedule Scheduled Booking

`POST /api/bookings/{bookingId}/reschedule`

Request:

```json
{
  "date": "2026-06-12",
  "slot": "10:00-11:00"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "allowed": true,
    "remainingReschedules": 2,
    "bookingId": 20482,
    "contractVersion": 1,
    "scheduledAt": "2026-06-12T04:30:00Z",
    "meta": {
      "trackingScreen": "SCHEDULED_STATUS",
      "showMap": false,
      "trackingRefreshSeconds": 0,
      "canCancel": true,
      "canReschedule": true,
      "canCallPartner": false,
      "canTrackPartner": false,
      "canRaiseIssue": false
    }
  }
}
```

## 4. Booking Details API

`GET /api/bookings/details/{bookingId}`

This API is the single source for:

- Booking lifecycle
- Tracking screen
- Partner info
- Map info
- Timeline
- Permissions
- Payment retry behavior
- Polling interval

Example when partner is on the way:

```json
{
  "success": true,
  "message": "Booking details fetched successfully",
  "data": {
    "id": 20481,
    "bookingId": 20481,
    "contractVersion": 1,
    "bookingType": "INSTANT",
    "status": "ON_THE_WAY",
    "assignmentStatus": "ON_THE_WAY",
    "paymentStatus": "SUCCESS",
    "retryPaymentAllowed": false,
    "serviceName": "Home Cleaning",
    "createdAt": "2026-06-08T13:00:00Z",
    "promisedStartTime": "2026-06-08T13:30:00Z",
    "fullAddress": "Sector 45, Gurugram",
    "totalAmount": 499,
    "estimatedArrivalMinutes": 12,
    "estimatedCompletionTime": "11:30 AM",
    "partnerAvailable": true,
    "partnerId": "77",
    "partnerName": "Rahul Kumar",
    "partnerPhone": "+919999999999",
    "partnerRating": 4.8,
    "arrivedAt": null,
    "startedAt": null,
    "inProgressAt": null,
    "completedAt": null,
    "map": {
      "enabled": true,
      "provider": "MAPBOX",
      "partner": {
        "id": 77,
        "latitude": 28.461,
        "longitude": 77.03,
        "updatedAt": "2026-06-08T13:20:00Z",
        "lastHeartbeatAt": "2026-06-08T13:20:00Z"
      },
      "customer": {
        "latitude": 28.4595,
        "longitude": 77.0266
      }
    },
    "statusHistory": [
      {
        "status": "PARTNER_ASSIGNED",
        "time": "2026-06-08T13:05:00Z"
      },
      {
        "status": "ON_THE_WAY",
        "time": "2026-06-08T13:15:00Z"
      }
    ],
    "meta": {
      "trackingScreen": "LIVE_TRACKING",
      "showMap": true,
      "trackingRefreshSeconds": 5,
      "canCancel": true,
      "canReschedule": false,
      "canCallPartner": true,
      "canTrackPartner": true,
      "canRaiseIssue": false
    }
  }
}
```

## 5. Tracking Screen Engine

Supported `meta.trackingScreen` values:

```text
INSTANT_STATUS
LIVE_TRACKING
PARTNER_ARRIVED
SERVICE_STARTED
SERVICE_IN_PROGRESS
SERVICE_COMPLETED
SCHEDULED_STATUS
SUBSCRIPTION_DETAILS
```

Backend mapping:

```text
PARTNER_ASSIGNED    -> INSTANT_STATUS
ON_THE_WAY          -> LIVE_TRACKING
ARRIVED             -> PARTNER_ARRIVED
SERVICE_STARTED     -> SERVICE_STARTED
SERVICE_IN_PROGRESS -> SERVICE_IN_PROGRESS
COMPLETED           -> SERVICE_COMPLETED
```

Frontend must open screens using only:

```js
booking.meta.trackingScreen
```

Example:

```js
switch (booking.meta?.trackingScreen) {
  case "INSTANT_STATUS":
    return <InstantStatusScreen booking={booking} />;
  case "LIVE_TRACKING":
    return <LiveTrackingScreen booking={booking} />;
  case "PARTNER_ARRIVED":
    return <PartnerArrivedScreen booking={booking} />;
  case "SERVICE_STARTED":
    return <ServiceStartedScreen booking={booking} />;
  case "SERVICE_IN_PROGRESS":
    return <ServiceInProgressScreen booking={booking} />;
  case "SERVICE_COMPLETED":
    return <ServiceCompletedScreen booking={booking} />;
  case "SCHEDULED_STATUS":
    return <ScheduledStatusScreen booking={booking} />;
  case "SUBSCRIPTION_DETAILS":
    return <SubscriptionDetailsScreen booking={booking} />;
  default:
    return <BookingStatusFallback booking={booking} />;
}
```

## 6. Lifecycle And Polling

Lifecycle:

```text
PARTNER_ASSIGNED
ON_THE_WAY
ARRIVED
SERVICE_STARTED
SERVICE_IN_PROGRESS
COMPLETED
```

Timestamp fields:

```json
{
  "arrivedAt": "2026-06-08T10:55:00Z",
  "startedAt": "2026-06-08T11:00:00Z",
  "inProgressAt": "2026-06-08T11:05:00Z",
  "completedAt": "2026-06-08T12:00:00Z"
}
```

Recommended backend polling values:

```text
PARTNER_ASSIGNED = 15 seconds
ON_THE_WAY = 5 seconds
ARRIVED = 5 seconds
SERVICE_STARTED = 15 seconds
SERVICE_IN_PROGRESS = 30 seconds
COMPLETED = 0 seconds
```

Frontend must use:

```js
booking.meta.trackingRefreshSeconds
```

## 7. Map Rules

Frontend must load Mapbox only when both values are true:

```js
const shouldLoadMapbox =
  booking?.meta?.showMap === true &&
  booking?.map?.enabled === true;
```

Partner coordinates are only allowed here:

```json
{
  "map": {
    "partner": {
      "latitude": 28.461,
      "longitude": 77.03
    }
  }
}
```

Do not use or expect these root fields:

```text
partnerLatitude
partnerLongitude
customerLatitude
customerLongitude
showMap
trackingScreen
trackingRefreshSeconds
canCancel
canReschedule
canCallPartner
canTrackPartner
canRaiseIssue
```

Map disabled response:

```json
{
  "partnerAvailable": false,
  "map": {
    "enabled": false,
    "reason": "PARTNER_LOCATION_UNAVAILABLE"
  },
  "meta": {
    "trackingScreen": "LIVE_TRACKING",
    "showMap": false,
    "trackingRefreshSeconds": 15,
    "canCancel": true,
    "canReschedule": false,
    "canCallPartner": true,
    "canTrackPartner": false,
    "canRaiseIssue": false
  }
}
```

Map disabled reasons:

```text
PAYMENT_NOT_PAID
TRACKING_NOT_ACTIVE
PARTNER_NOT_ASSIGNED
PARTNER_LOCATION_UNAVAILABLE
UNAVAILABLE
```

## 8. Subscription Flow

Subscriptions use dedicated APIs. Do not reuse booking tracking APIs for subscription screens.

```text
GET  /api/subscriptions
GET  /api/subscriptions/{id}
GET  /api/subscriptions/{id}/visits
POST /api/subscriptions/{id}/pause
POST /api/subscriptions/{id}/resume
POST /api/subscriptions/{id}/cancel
```

Subscription response:

```json
{
  "success": true,
  "data": {
    "subscriptionId": 1,
    "planName": "Daily Cleaning",
    "frequency": "MONDAY,WEDNESDAY,FRIDAY",
    "frequencyDays": ["Monday", "Wednesday", "Friday"],
    "nextVisitDate": "2026-06-20",
    "remainingVisits": 12,
    "status": "ACTIVE",
    "bookingId": 501,
    "startDate": "2026-06-09",
    "endDate": "2026-07-09",
    "timeSlot": "10:00 AM",
    "totalAmount": 4999,
    "canPause": true,
    "canResume": false,
    "canCancel": true
  }
}
```

Visits response:

```json
{
  "success": true,
  "data": {
    "upcomingVisits": [],
    "completedVisits": [],
    "cancelledVisits": []
  }
}
```

Subscription statuses:

```text
PENDING
ACTIVE
PAUSED
CANCELLED
COMPLETED
```

Frontend must use subscription action permissions directly:

```js
subscription.canPause
subscription.canResume
subscription.canCancel
```

## 9. My Bookings History

`GET /api/bookings/history?page=1&size=20`

Response:

```json
{
  "success": true,
  "message": "Booking history fetched",
  "data": {
    "instantBookings": {
      "items": [],
      "page": 1,
      "size": 20,
      "hasMore": false
    },
    "scheduledBookings": {
      "items": [],
      "page": 1,
      "size": 20,
      "hasMore": false
    },
    "subscriptions": {
      "items": [],
      "page": 1,
      "size": 20,
      "hasMore": false
    }
  }
}
```

Do not mix subscription plans with regular bookings.

## 10. Error Codes

Errors include machine-readable `code`.

```json
{
  "success": false,
  "code": "BOOKING_NOT_FOUND",
  "message": "Booking not found"
}
```

Common codes:

```text
BOOKING_NOT_FOUND
PAYMENT_FAILED
PAYMENT_EXPIRED
SUBSCRIPTION_PAUSED
SUBSCRIPTION_CANCELLED
PARTNER_UNAVAILABLE
TRACKING_DISABLED
INVALID_REQUEST
INTERNAL_ERROR
```

Frontend should rely on `code`, not message parsing.

## 11. Frontend Final Rules

Use only these fields for UI decisions:

```js
booking.contractVersion
booking.statusHistory
booking.meta.trackingScreen
booking.meta.showMap
booking.meta.trackingRefreshSeconds
booking.meta.canCancel
booking.meta.canReschedule
booking.meta.canCallPartner
booking.meta.canTrackPartner
booking.meta.canRaiseIssue
booking.map?.enabled
booking.map?.reason
booking.map?.partner
booking.partnerAvailable
booking.paymentStatus
booking.retryPaymentAllowed
booking.arrivedAt
booking.startedAt
booking.inProgressAt
booking.completedAt
subscription.canPause
subscription.canResume
subscription.canCancel
```

Never do this on frontend:

```js
if (booking.status === "ON_THE_WAY") {
  openLiveTrackingScreen();
}
```

Do this instead:

```js
openScreen(booking.meta.trackingScreen);
```

## 12. API Freeze Checklist

Implemented contract requirements:

- `contractVersion` present in booking responses
- `statusHistory` present in booking responses
- `meta` object present in booking responses
- UI-control fields live only inside `meta`
- Tracking screen controlled by backend
- `PARTNER_ARRIVED` tracking screen supported
- `ARRIVED` lifecycle state supported
- `SERVICE_IN_PROGRESS` lifecycle state supported
- Partner coordinates live only inside `map.partner`
- Map disabled reasons standardized
- Payment retry behavior returned by backend
- Booking history is grouped and paginated
- Subscription APIs are separate from booking tracking APIs
- Subscription permissions returned explicitly
- Error codes standardized
- OpenAPI freeze spec maintained in `docs/booking-flow-openapi.yaml`

## 13. Partner App Backend Contract

This section lists the backend currently available for the Lexho Partner app. Unless noted otherwise, partner APIs require a valid partner JWT and `ROLE_PARTNER` or `ROLE_ADMIN`.

Base URL used in local development:

```text
http://localhost:8082
```

Common headers:

```http
Authorization: Bearer <partner-jwt>
Content-Type: application/json
```

Multipart upload APIs use `multipart/form-data`.

### 13.1 Implemented Partner Modules

Implemented partner app areas:

- Partner profile read and update
- Partner profile image upload
- Partner document upload
- Partner online or offline availability
- Partner shift start, end, and history
- Partner live location update
- Partner skills read and update
- New order listing, detail, accept, reject, start, complete, and history
- Partner incoming request listing, detail, accept, reject, and dev seed
- Partner dashboard stats
- Partner banners
- Partner earnings summary and transactions
- Partner payouts
- Partner bank details
- Partner withdrawal request and balance
- Partner notifications list, mark-read, and unread count
- Customer/admin nearby partner discovery
- Partner live tracking SSE
- Compatibility routes for older partner app clients

### 13.2 Profile APIs

Partner profile APIs are exposed under both `/api/partner` and `/api/v1/partner`.

```text
GET  /api/v1/partner/profile
PUT  /api/v1/partner/profile
POST /api/v1/partner/profile-image
POST /api/v1/partner/documents
PUT  /api/v1/partner/availability
GET  /api/v1/partner/dashboard/stats
GET  /api/v1/partner/banners?page=0&size=5
```

Update profile request:

```json
{
  "fullName": "Partner Name",
  "phone": "9999999999",
  "email": "partner@example.com",
  "address": "Partner address",
  "city": "Delhi",
  "pincode": "110001"
}
```

Availability request:

```json
{
  "available": true
}
```

Document upload fields:

```text
aadhaar      required file
pan          required file
selfie       required file
certificate  optional file
```

### 13.3 Order APIs

Primary order APIs are exposed under both `/api/orders` and `/api/v1/orders`.

```text
GET  /api/v1/orders/new?page=0&size=20&sortBy=createdAt&direction=desc
GET  /api/v1/orders/{id}
POST /api/v1/orders/{id}/accept
POST /api/v1/orders/{id}/reject
POST /api/v1/orders/{id}/start
POST /api/v1/orders/{id}/complete
GET  /api/v1/orders/history?page=0&size=20&sortBy=createdAt&direction=desc
GET  /api/v1/orders/dashboard
```

Start order upload:

```text
beforeImage  required file
```

Complete order upload:

```text
afterImage  required file
```

Order response contains customer, address, coordinates, service, amount, payment mode, status, assigned partner, and created time fields.

### 13.4 Partner Request APIs

Partner request APIs are exposed under both `/api/partner` and `/api/v1/partner`.

```text
PATCH /api/v1/partner/me/status
GET   /api/v1/partner/requests?status=&page=0&size=20
GET   /api/v1/partner/requests/{requestId}
PATCH /api/v1/partner/requests/{requestId}/accept
PATCH /api/v1/partner/requests/{requestId}/reject
PATCH /api/v1/partner/requests/dev/seed
```

Reject request body:

```json
{
  "reason": "Not available"
}
```

Incoming request response includes request id, cart id, customer id, service id, service type, quantity, scheduled time, status, created time, ETA, expiry time, and address.

Accept and reject APIs are rate limited per partner.

### 13.5 Dispatch APIs

Dispatch APIs are exposed under both `/api/partner/dispatch` and `/api/v1/partner/dispatch`.

```text
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

Shift start request:

```json
{
  "shiftStart": "2026-06-09T09:00:00",
  "shiftEnd": "2026-06-09T18:00:00"
}
```

Dispatch availability request:

```json
{
  "isOnline": true,
  "maxActiveJobs": 3
}
```

Location update request:

```json
{
  "latitude": 28.6139,
  "longitude": 77.209,
  "source": "gps",
  "trackingState": "AVAILABLE",
  "accuracyMeters": 12.5,
  "speedKmh": 18.0,
  "headingDegrees": 90.0,
  "mockLocation": false,
  "rootedDevice": false
}
```

Skills update request:

```json
{
  "skills": ["PLUMBING", "ELECTRICIAN"]
}
```

Redis is used for geo indexing and nearby search. If Redis is not running locally, location and availability APIs should not crash, but nearby partner discovery may return empty results until Redis is available.

### 13.6 Compatibility Routes

Older partner app clients can use these compatibility routes under `/api/partner` and `/api/v1/partner`.

```text
GET  /api/v1/partner/orders/new
GET  /api/v1/partner/orders/{id}
GET  /api/v1/partner/dispatch/new
GET  /api/v1/partner/dispatch/requests/new
POST /api/v1/partner/dispatch/{requestId}/accept
POST /api/v1/partner/dispatch/{requestId}/reject
```

Frontend should prefer the primary `/api/v1/orders` and `/api/v1/partner/requests` routes for new work.

### 13.7 Earnings, Payout, And Bank APIs

These APIs are exposed under both `/api/partner` and `/api/v1/partner`.

```text
GET  /api/v1/partner/earnings/summary
GET  /api/v1/partner/earnings/transactions?fromDate=&toDate=&page=0&size=20&sortBy=updatedAt&direction=desc
GET  /api/v1/partner/payouts?page=0&size=20
GET  /api/v1/partner/payouts/{payoutId}
GET  /api/v1/partner/bank-details
PUT  /api/v1/partner/bank-details
POST /api/v1/partner/withdrawals
GET  /api/v1/partner/withdrawals?page=0&size=20
GET  /api/v1/partner/withdrawals/balance
```

Bank details request:

```json
{
  "accountHolderName": "Partner Name",
  "accountNumber": "1234567890",
  "ifscCode": "HDFC0000001",
  "bankName": "HDFC Bank"
}
```

Withdrawal request:

```json
{
  "amount": 500.00
}
```

### 13.8 Notification APIs

Notification APIs support partner and provider route aliases.

```text
GET   /api/v1/partner/notifications?page=0&size=20
PATCH /api/v1/partner/notifications/{id}/read
GET   /api/v1/partner/notifications/unread-count
GET   /api/provider/notifications?page=0&size=20
GET   /api/v1/provider/notifications?page=0&size=20
```

Use `/api/v1/partner/notifications` for new frontend integration.

### 13.9 Discovery And Live Tracking

Nearby partner discovery is available for customer/admin clients.

```text
POST /api/v1/partners/nearby
```

Nearby request:

```json
{
  "lat": 28.6139,
  "lng": 77.209,
  "serviceType": "PLUMBING",
  "radiusKm": 5
}
```

Live partner tracking is exposed as Server-Sent Events.

```text
GET /api/v1/partners/live/{partnerId}
Accept: text/event-stream
```

SSE event names:

```text
partner-location
heartbeat
```

Live tracking payload includes partner id, online flag, tracking state, latitude, longitude, geohash, pincode, sequence number, last seen time, speed, and heading when available.

### 13.10 Runtime Dependencies

Required local services:

```text
MySQL  localhost:3306
Redis  localhost:6379
```

The application currently starts on:

```text
server.port=8082
```

If port 8082 is busy, stop the process using that port before starting the Spring Boot app.

If Redis is not running, these features are affected:

- Rate limiter falls back to local behavior
- Partner geo index writes are skipped
- Nearby partner discovery can return no partners
- Availability and location APIs should continue returning application responses after the Redis fallback patch

### 13.11 Partner App Frontend Checklist

Use these routes for the current partner app:

```text
GET   /api/v1/partner/profile
PUT   /api/v1/partner/profile
GET   /api/v1/partner/dashboard/stats
GET   /api/v1/partner/banners
GET   /api/v1/orders/new
GET   /api/v1/orders/{id}
POST  /api/v1/orders/{id}/accept
POST  /api/v1/orders/{id}/reject
POST  /api/v1/orders/{id}/start
POST  /api/v1/orders/{id}/complete
GET   /api/v1/orders/history
GET   /api/v1/partner/requests
PATCH /api/v1/partner/requests/{requestId}/accept
PATCH /api/v1/partner/requests/{requestId}/reject
PATCH /api/v1/partner/dispatch/availability
POST  /api/v1/partner/dispatch/location
GET   /api/v1/partner/notifications
PATCH /api/v1/partner/notifications/{id}/read
GET   /api/v1/partner/earnings/summary
GET   /api/v1/partner/earnings/transactions
GET   /api/v1/partner/withdrawals/balance
POST  /api/v1/partner/withdrawals
```

Frontend rules:

- Use `/api/v1/...` routes for new screens
- Keep `/api/...` and provider aliases only for backward compatibility
- Send partner JWT on every protected API
- Use multipart only for image and document upload APIs
- Do not assume Redis-backed nearby results are available in local development unless Redis is running



