# Lexho App Status README

This file summarizes what is currently implemented in this repository for the `admin app`, `partner app`, and `customer app`.

Important scope note:

- This repository primarily contains backend services and contracts.
- Frontend/mobile app source for admin, partner, and customer apps is not part of this repo.
- So "bana huaa hai" here means backend APIs, business flows, data models, migrations, and integration contracts that those apps can use.

Base local runtime:

- Backend base URL: `http://localhost:8082`
- Swagger UI: `/swagger-ui.html`
- OpenAPI docs: `/v3/api-docs`
- Health: `/actuator/health`

## Shared Auth and Platform Readiness

Implemented shared platform capabilities:

- OTP auth for customer and partner
- Refresh-token flow
- Admin email/password login with OTP verification
- JWT role protection
- Global exception handling
- Standard error response contract
- Request ID and correlation ID propagation
- Structured logging
- Audit log support
- Razorpay webhook handling
- Payment event idempotency
- FCM device token support
- WebSocket/SSE notification infrastructure
- Actuator metrics and Prometheus endpoints

Primary auth routes:

- `POST /otp/send`
- `POST /otp/verify`
- `POST /api/auth/refresh`
- `POST /auth/refresh-token`
- `POST /api/admin/login`
- `POST /api/admin/verify-otp`

Role protection:

- `/api/admin/**` and `/api/v1/admin/**` -> `ROLE_ADMIN`
- `/api/customer/**` and `/api/v1/customer/**` -> `ROLE_CUSTOMER`
- `/api/partner/**` and `/api/v1/partner/**` -> `ROLE_PARTNER`

## Customer App

Customer app backend coverage currently available:

- OTP login and refresh token flow
- Home screen content
- Home banners
- Dynamic home sections
- Service categories and service listing
- Service detail APIs
- Popular services
- Cart add/update/remove
- Add-on support in cart and order flows
- Cart config support
- Cook family-size pricing support
- Location save/update
- Address management
- Reverse geocoding
- Serviceability checks
- Geofence-backed serviceability rules
- Instant booking flow
- Scheduled booking flow
- Booking details and tracking metadata
- Booking history
- Subscription flow
- Subscription pricing, visits, pause/resume/cancel, skip/reschedule
- Payment options, order creation, verification, retry, latest payment status
- Refund initiation API
- Customer profile APIs
- Device token registration
- Customer reviews
- Notification channel support
- Live tracking metadata in booking responses

Main customer API groups:

- Auth: `POST /otp/send`, `POST /otp/verify`, `POST /api/auth/refresh`
- Home: `GET /api/customer/banners`, `GET /api/customer/home/banners`, `GET /api/customer/home/sections`
- Catalogue: `GET /api/customer/services`, `GET /api/customer/services/{id}`, `GET /api/categories/**`
- Cart: `/api/customer/cart/**`
- Location/address: `/api/customer/location/**`, `/api/v1/customer/location/**`, `/api/customer/addresses/**`
- Booking/order: `POST /api/v1/bookings/create`, booking detail/history/order routes
- Subscription: `/api/subscription/**`, `/api/subscriptions/**`
- Payments: `/api/payments/options`, `/api/payments/create-order`, `/api/payments/verify`, `/api/payments/retry`, `/api/payments/{bookingId}`, `/api/payments/refund`
- Profile: `/api/customer/profile/**`
- Reviews: `POST /api/v1/customer/reviews`, `GET /api/v1/customer/reviews/{bookingId}`

Customer app UI-enabling backend behavior already present:

- Booking response `meta` object controls tracking screen behavior
- Map visibility is backend-driven
- Polling intervals are backend-driven
- Payment retry behavior is backend-driven
- Subscription permissions are returned explicitly
- Error responses include machine-readable codes

Customer-facing controllers present in repo include:

- `LexhoAuthController`
- `LexhoExperienceController`
- `CustomerServiceController`
- `CustomerCartFallbackController`
- `CustomerLocationController`
- `CustomerReviewController`
- customer profile, banner, section, subscription, payment, and order controllers referenced from the main backend module

## Partner App

Partner app backend coverage currently available:

- Partner OTP login and refresh token flow
- Partner onboarding
- Document upload
- Profile read/update
- Profile image upload
- KYC workflow
- Bank details get/update
- Bank verification status flow
- Availability update
- Shift start/end/history
- Dispatch configuration
- Skills update/get
- Live location update
- Auto-offline rules
- New order listing
- Order detail
- Order accept/reject/start/complete
- Before/after service image upload
- Order timeline and history
- Partner request list/detail
- Request accept/reject
- Request expiry handling
- Dashboard stats
- Partner banners
- Notifications list/mark-read/unread count
- FCM device token registration and delete
- Earnings summary
- Earnings transaction history
- Payout list/detail
- Withdrawal balance and request flow
- Ratings and reviews
- Leave management
- Service area preferences
- Penalty/governance support
- Nearby partner discovery
- Live partner tracking SSE
- Settlement reversal hooks from refund flow

Main partner API groups:

- Auth: `POST /otp/send`, `POST /otp/verify`, `POST /api/auth/refresh`
- Profile: `GET /api/v1/partner/profile`, `PUT /api/v1/partner/profile`, `POST /api/v1/partner/profile-image`
- Documents/KYC: `POST /api/v1/partner/documents`
- Dashboard: `GET /api/v1/partner/dashboard/stats`, `GET /api/v1/partner/banners`
- Orders: `GET /api/v1/orders/new`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/accept`, `POST /api/v1/orders/{id}/reject`, `POST /api/v1/orders/{id}/start`, `POST /api/v1/orders/{id}/complete`, `GET /api/v1/orders/{id}/timeline`, `GET /api/v1/orders/history`
- Requests: `GET /api/v1/partner/requests`, `GET /api/v1/partner/requests/{requestId}`, `PATCH /api/v1/partner/requests/{requestId}/accept`, `PATCH /api/v1/partner/requests/{requestId}/reject`
- Dispatch: `GET /api/v1/partner/dispatch/config`, `POST /api/v1/partner/dispatch/shift/start`, `POST /api/v1/partner/dispatch/shift/end`, `PATCH /api/v1/partner/dispatch/availability`, `POST /api/v1/partner/dispatch/location`, `PUT /api/v1/partner/dispatch/skills`, `GET /api/v1/partner/dispatch/skills`
- Notifications: `GET /api/v1/partner/notifications`, `PATCH /api/v1/partner/notifications/{id}/read`, `GET /api/v1/partner/notifications/unread-count`
- Device token: `POST /api/v1/partner/device-token`, `DELETE /api/v1/partner/device-token`
- Earnings/payouts: `GET /api/v1/partner/earnings/summary`, `GET /api/v1/partner/earnings/transactions`, `GET /api/v1/partner/payouts`, `GET /api/v1/partner/payouts/{payoutId}`
- Bank/withdrawal: `GET /api/v1/partner/bank-details`, `PUT /api/v1/partner/bank-details`, `POST /api/v1/partner/withdrawals`, `GET /api/v1/partner/withdrawals`, `GET /api/v1/partner/withdrawals/balance`
- Leave/service area: `POST /api/v1/partner/leave`, `GET /api/v1/partner/leave`, `GET /api/v1/partner/service-areas`, `PUT /api/v1/partner/service-areas`

Partner-facing controllers present in repo include:

- `LexhoPartnerController`
- `LexhoPartnerOrderController`
- `LexhoPartnerNotificationController`
- `PartnerDispatchController`
- `PartnerDispatchCompatibilityController`
- `PartnerRequestController`
- `PartnerLiveTrackingController`
- `ProviderOnboardingController`
- `ProviderReviewController`

Partner services present in repo include:

- `PartnerProfileService`
- `PartnerOrderService`
- `PartnerOperationsService`
- `PartnerSettlementService`
- `PartnerDeviceTokenService`
- `PartnerBannerService`
- `AdminPartnerGovernanceService`

## Admin App

Admin app backend coverage currently available:

- Admin login with OTP verification
- Admin JWT role protection
- Dashboard stats and analytics
- Revenue analytics
- Service analytics and top services
- Category management
- Service management
- Service detail management
- Media/image upload
- Home banner management
- Popular service management
- Section and section-item management
- Add-on management
- Service/category add-on mapping
- Cart config management
- Cook family-size management
- Cleaning coverage management
- Cook coverage management
- Subscription pricing management
- Subscription management
- Subscription calendar and visits
- Partner assignment and replacement for subscriptions
- Customer management
- Customer delete request handling
- User management
- Partner onboarding governance
- Partner KYC review
- Partner bank verification review
- Partner penalty management
- Partner settlements and withdrawal admin controls
- Booking management
- Booking status operations
- Tracking and reconciliation operations
- Payment dashboard and refund/reconciliation flows
- Geo city and zone management
- Serviceable area management
- Restricted area operations
- Scale profile operations
- Notification management and bulk notifications
- Review moderation and analytics
- Admin settings

Main admin API groups:

- Auth: `POST /api/admin/login`, `POST /api/admin/verify-otp`
- Dashboard: `/api/admin/dashboard/**`, `/api/v1/admin/dashboard/**`
- Categories/services: `/api/admin/categories`, `/api/admin/services`, admin management routes
- Service details: `/api/admin/services/{serviceId}/details/**`
- Upload/media: `/api/admin/upload/image`
- Home banners: `/api/admin/home-banners/**`
- Popular services: admin popular-service routes
- Sections: `/api/admin/sections/**`
- Add-ons: `/api/admin/addons/**`
- Cart config: `/api/admin/cart-config`
- Pricing/coverage: `/api/admin/cook-family-sizes/**`, `/api/admin/cleaning-coverage/**`, `/api/admin/cook-coverage/**`, `/api/admin/subscription-pricing/**`
- Subscriptions: `/api/admin/subscriptions/**`, `/api/v1/admin/subscriptions/**`
- Customers: `/api/admin/customers/**`, `/api/v1/admin/customers/**`
- Users: `/api/admin/users/**`
- Partners: `/api/admin/partners/**`, `/api/v1/admin/partners/**`
- Booking management: `/api/admin/bookings/**`, `/api/v1/admin/bookings/**`, `/api/v1/admin/booking-management/**`
- Payments: `/api/admin/payments/**`
- Geo/serviceability: `/api/admin/geo/**`, serviceable-area routes, restricted-area routes, scale-profile routes
- Notifications: `/api/admin/notifications/**`
- Reviews: `/api/admin/reviews/**`, `/api/v1/admin/reviews/**`
- Settings: admin settings routes

Admin-facing controllers present in repo include:

- `AdminAuthController`
- `AdminDashboardController`
- `AdminManagementController`
- `AdminPopularServiceController`
- `AdminSectionController`
- `AdminBookingManagementController`
- `AdminPaymentController`
- `AdminCustomerManagementController`
- `AdminPartnerControlController`
- `AdminNotificationController`
- `AdminReviewController`
- `AdminSettingsController`
- geo, coverage, banner, add-on, subscription, and user-management admin controllers

## Payment, Notifications, Geo, and Dispatch Support

These app-supporting backend systems are also implemented:

- Razorpay order create/verify/retry
- Refund initiation and refund webhook handling
- Partner settlement ledger and reversal handling
- Admin payment reconciliation
- FCM push service integration
- Device token registration for customer and partner sides
- Admin bulk notifications
- WebSocket notification support
- Customer current location and reverse geocoding
- Serviceability checks
- Geofence validation
- Partner geo indexing
- Nearby partner lookup
- Partner tracking lifecycle
- Live tracking endpoints

## Database and Migration Signals

The repo includes migrations for the above app areas, including:

- user management
- customer cart and banners
- dynamic sections
- serviceable areas
- dispatch foundation
- customer location management
- partner marketplace core
- partner skill mapping
- payout and settlement ledger
- partner governance controls
- partner app production features
- emergency and geo support

Examples present in repo:

- `V21__partner_service_requests.sql`
- `V29__lexho_partner_marketplace_core.sql`
- `V32__partner_skill_mapping.sql`
- `V47__partner_admin_governance_controls.sql`
- `V60__partner_settlement_ledger.sql`
- `V80__partner_app_production_features.sql`

## What This README Means in Practice

If you are planning frontend work:

- `admin app` backend is broadly ready across dashboard, catalog, bookings, partners, payments, geo, notifications, and reviews.
- `partner app` backend is broadly ready across profile, KYC, orders, dispatch, payouts, notifications, leave, and service areas.
- `customer app` backend is broadly ready across auth, home, services, cart, booking, payment, subscriptions, location, profile, and reviews.

What is not guaranteed by this file:

- every screen is already built in a frontend repo
- every route has been E2E-tested on a live deployment
- all old compatibility aliases should be used for new frontend work

For new frontend integration, prefer `/api/v1/...` routes where they exist.

## Source Documents

This file is consolidated from current repository docs and code, mainly:

- [README.md](C:/Users/shivendra/Downloads/lexho-user-service-master/README.md)
- [docs/PARTNER_APP_README.md](C:/Users/shivendra/Downloads/lexho-user-service-master/docs/PARTNER_APP_README.md)
- [ADMIN_FRONTEND_INTEGRATION.md](C:/Users/shivendra/Downloads/lexho-user-service-master/ADMIN_FRONTEND_INTEGRATION.md)
- [USER_MANAGEMENT_MODULE.md](C:/Users/shivendra/Downloads/lexho-user-service-master/USER_MANAGEMENT_MODULE.md)
- [FRONTEND_TOKEN_MATRIX.md](C:/Users/shivendra/Downloads/lexho-user-service-master/FRONTEND_TOKEN_MATRIX.md)
