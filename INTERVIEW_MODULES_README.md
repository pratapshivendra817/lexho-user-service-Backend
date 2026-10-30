# Lexho Backend Modules - Interview Notes

Ye document interview preparation ke liye banaya gaya hai. Isme repository ke major modules, unka purpose, responsibilities, tech stack, data flow, APIs, security, testing aur interview talking points cover kiye gaye hain.

## Project Overview

Lexho backend ek Spring Boot based multi-module Maven project hai. Codebase ka main goal home-service/booking platform ke backend ko support karna hai jisme customer app, partner app, admin panel, booking, dispatch, payment, notification, support aur CMS-type features included hain.

High-level architecture:

- Parent project: `lexho-parent`
- Runtime app: `application-module`
- Shared code: `common-lib`
- Domain modules: `auth-service`, `user-service`, `order-service`, `payment-service`, `partner-module`, `catalog-module`, `notification-module`, `admin-module`
- Reporting module: `coverage-report`
- Database migration: Flyway SQL scripts
- Main database: MySQL
- Optional infrastructure: Redis, RabbitMQ-style geo event config, Razorpay, FCM, email/SMS gateway, Mapbox/Nominatim

## Tech Stack

- Java 17
- Spring Boot 3.5.x
- Spring MVC REST APIs
- Spring Data JPA / Hibernate
- Spring Security + JWT
- Bean Validation
- Flyway database migrations
- MySQL connector
- Redis cache/config support
- Springdoc OpenAPI / Swagger UI
- Actuator + Micrometer metrics
- Razorpay Java SDK
- Netty Socket.IO / WebSocket support
- JUnit 5, Mockito, integration tests
- JaCoCo aggregate coverage
- Checkstyle and SpotBugs quality gates

## Build And Runtime

Default backend runtime is handled by `application-module`.

Common commands:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -pl application-module spring-boot:run
```

Runtime details:

- Default port: `8082`
- Application config: `application-module/src/main/resources/application.properties`
- Environment files: `.env`, `.env.example`, `.env.staging.example`, `.env.production.example`
- API docs: `/swagger-ui.html`, `/v3/api-docs`
- Health endpoints: `/actuator/health`, `/health`, `/health/liveness`, `/health/readiness`
- Metrics: `/actuator/prometheus`, `/actuator/metrics`

## Module Summary

| Module | Purpose | Interview Explanation |
|---|---|---|
| `common-lib` | Shared models/utilities | Common contracts and reusable classes used by multiple backend modules. |
| `auth-service` | Token/auth module | Issues, refreshes and revokes internal auth tokens. Also supports JWT-based authentication flow. |
| `admin-module` | Admin domain base | Admin entity/repository support and admin-related persistence layer. |
| `user-service` | User and calling service | Admin user management, JWT security, customer/user entities, call sessions, call logs, socket/realtime calling support. |
| `order-service` | Booking/order management | Admin booking APIs, booking entity logic, status transitions and order lifecycle support. |
| `payment-service` | Payment gateway | Razorpay order creation, retry, verification, webhook processing, refunds, reconciliation and payment audit records. |
| `partner-module` | Partner domain | Partner profile, marketplace, onboarding, service mapping, availability and partner admin support. |
| `catalog-module` | Catalog domain | Category/service catalog persistence and catalog data support. |
| `notification-module` | Notification domain | Notification entities and delivery-related persistence support. |
| `application-module` | Main runnable app | Aggregates all modules into one deployable Spring Boot service and contains most customer, partner, admin and operational APIs. |
| `coverage-report` | Quality/reporting | Aggregates JaCoCo coverage across all modules. |

## Application Module

`application-module` is the main runtime module. It depends on all business modules and exposes the combined REST API.

Important responsibilities:

- Customer app APIs
- Partner app APIs
- Admin panel APIs
- Booking and dispatch flows
- Subscription and service selection flows
- Payment integration wiring
- Notification, support, FAQ, legal content and CMS APIs
- Health, observability and operational endpoints
- Flyway migration execution

Major feature packages:

- `admin`: admin dashboard, settings, service management, booking management, customer management, moderation, monitoring
- `customer`: customer profile, location, cart, banners, referrals and app-facing features
- `lexho`: customer app auth/profile/address/experience APIs
- `lexhopartner`: partner app profile, order, support and notification APIs
- `dispatch`: partner dispatch, live assignment and compatibility APIs
- `map`: map, ETA, tracking and navigation APIs
- `subscription`: subscription booking, pricing, visit and partner assignment logic
- `payment`: payment integration facade
- `notification`: push tokens, notification delivery and lifecycle
- `support`: support tickets, replies and public support APIs
- `emergency`: partner SOS flow and admin escalation
- `legal` / `legalcontent`: legal content and acceptance
- `website`: public website header/navigation content
- `localization`: partner app translations and dynamic content localization
- `observability`: metrics/audit/ops support

Interview line:

> The application is packaged as a single Spring Boot runtime, but the code is divided by business capability. This gives modular ownership while keeping deployment simple.

## Auth Service

Path: `auth-service`

Main controller:

- `AuthTokenController`

Important API base:

- `POST /internal/auth/token`
- `POST /internal/auth/refresh`
- `POST /internal/auth/revoke`

Responsibilities:

- Generate access tokens for internal/service use cases
- Refresh tokens
- Revoke tokens
- Use JWT library dependencies
- Share auth contracts with the main runtime

Interview points:

- JWT keeps APIs stateless.
- Refresh-token flow allows short-lived access tokens and longer-lived sessions.
- Token revocation is separated from normal API business logic.

## User Service

Path: `user-service`

Main controllers:

- `AdminUserManagementController`
- `CallController`

Important API bases:

- `/api/admin/users`
- `/api/admin/customer-profiles`
- `/calls`

Responsibilities:

- User/customer admin management
- User create/update/block/unblock/delete
- User detail and paginated listing
- JWT security filter and current-user resolution
- Call sessions and call history
- Missed call and call-end flows
- Socket.IO support for realtime call signalling
- Booking-service client integration

Important packages:

- `security`: JWT authentication, current user, security config
- `service`: user management, call service, rate limiter, timeout scheduler
- `socket`: Socket.IO config, socket auth, event publishing
- `entity`: `User`, `CallSession`, `CallLog`
- `repository`: Spring Data repositories

Interview points:

- Security is centralized with JWT filter and user-context service.
- Calling flow stores durable call sessions/logs and publishes realtime events through socket publisher abstraction.
- Rate limiting and timeout scheduling protect call APIs from abuse and stuck sessions.

## Order Service

Path: `order-service`

Main controller:

- `AdminBookingController`

Important API bases:

- `/api/admin/bookings`
- `/api/v1/admin/bookings`

Responsibilities:

- Booking/order admin listing
- Booking stats
- Booking detail lookup
- Booking update/cancel/delete
- Booking status lifecycle
- Integration with user and catalog modules

Interview points:

- Booking is treated as a core lifecycle entity.
- Admin APIs support operational actions like status updates, cancellation and investigation.
- Order module integrates with payment and dispatch through domain boundaries.

## Payment Service

Path: `payment-service`

Main controllers:

- `PaymentController`
- `AdminPaymentController`
- `RazorpayWebhookController`

Important API bases:

- `/api/payments`
- `/api/v1/payments`
- `/api/v1/admin/payments`
- `/api/v1/webhooks/razorpay`

Responsibilities:

- Create Razorpay payment orders
- Retry failed payments
- Verify payment signatures
- Read payment status by booking
- Expose payment options
- Admin dashboard and transaction listing
- Payment reconciliation
- Refund creation
- Webhook processing for captured/failed/refund events
- Idempotency and retry tracking for webhooks

Important entities:

- `PaymentTransaction`
- `PaymentRefund`
- `PaymentWebhookEvent`

Interview points:

- Webhook processing is idempotent to avoid double updates.
- Razorpay signatures are verified before trusting payment events.
- Payment state is persisted separately from booking state, then booking can react to payment events.
- Admin reconciliation exists for production support.

## Partner Module

Path: `partner-module`

Responsibilities:

- Partner profile and onboarding data
- Partner service mapping
- Partner marketplace readiness
- Partner status/governance support
- Partner admin support contracts
- Integration with user and catalog modules

Related application packages:

- `lexhopartner`
- `partnerrequest`
- `dispatch`
- `map`
- `localization`
- `emergency`

Interview points:

- Partner domain is separated because partner lifecycle is different from customer lifecycle.
- Dispatch depends on partner availability, skills, location freshness and serviceability rules.
- Partner app has separate APIs for orders, support, notifications and localization.

## Catalog Module

Path: `catalog-module`

Responsibilities:

- Category and service catalog persistence
- Service/subcategory data support
- Integration with admin management and customer service listing

Related application packages:

- `admin` service/category management
- `controller/SubCategoryController`
- `customer/CustomerServiceController`
- `addon`
- `section`
- `homebanner`
- `popular services`

Interview points:

- Catalog drives what customer can book.
- Admin manages categories, services, subcategories, add-ons and display order.
- Customer app consumes filtered/active catalog views.

## Notification Module

Path: `notification-module`

Responsibilities:

- Notification persistence
- Push token lifecycle
- Delivery support through optional FCM/email/SMS configuration
- Notification templates and language-specific uniqueness

Related application controllers:

- `NotificationSendController`
- `PushTokenController`
- `LexhoPartnerNotificationController`
- `AdminNotificationController`

Interview points:

- Notification system stores delivery state and device tokens.
- Config supports FCM, email and SMS without forcing them in local development.
- Idempotency and lifecycle migrations protect duplicate notification sends.

## Admin Module

Path: `admin-module`

Responsibilities:

- Admin entity and repository
- Admin persistence foundation
- Used by application admin APIs

Related application features:

- Admin login and OTP verification
- Admin dashboard
- Admin settings
- Admin customer/partner/booking/payment management
- Admin audit logs
- Admin serviceability and geofence management
- Admin support and emergency handling

Interview points:

- Admin APIs are operational APIs, not customer-facing APIs.
- Admin actions should be auditable because they affect bookings, payments, partners and customers.

## Common Lib

Path: `common-lib`

Responsibilities:

- Shared classes used by multiple modules
- Common API/domain utilities
- Reduces duplication across modules

Interview points:

- Common code is separated only for genuinely shared behavior.
- Business-specific logic should remain inside its owning module.

## Coverage Report Module

Path: `coverage-report`

Responsibilities:

- Aggregates JaCoCo reports across modules
- Helps measure test coverage at whole-project level

Interview points:

- Multi-module coverage is harder than single-module coverage; aggregate reporting gives one quality view.

## Database And Migrations

Flyway is enabled in the main runtime.

Migration locations:

- `classpath:db/migration/application-module`
- `classpath:db/migration/user-service`

This repository has a large migration history for:

- Admin module
- Catalog management
- Booking management
- Customer profiles and locations
- Partner marketplace and onboarding
- Dispatch foundation
- Geo tracking and serviceability
- Razorpay payment transactions and refunds
- Subscription booking APIs
- Notification lifecycle
- Support ticket replies and attachments
- Legal content CMS
- Referral and coupon system
- Account deletion compliance
- Booking idempotency and payment/dispatch safety

Interview points:

- Flyway provides repeatable database evolution.
- `ddl-auto=validate` means Hibernate validates schema instead of silently changing production schema.
- Migrations show production-style schema control.

## Security

Security capabilities present in the project:

- JWT access tokens
- Refresh-token support
- Spring Security filters
- Admin OTP login
- Password lock/rate-limit configuration for admin login
- Request validation
- Role/context based protected APIs
- Razorpay webhook signature verification
- Hidden server error messages in runtime config

Interview points:

- API security is split into authentication, authorization, validation and external webhook verification.
- Sensitive runtime values are configured through environment variables.
- Admin flows have additional rate-limit and lockout settings.

## Observability And Production Readiness

Production support features:

- Actuator health/info/metrics/prometheus endpoints
- Request ID and correlation ID support
- Audit log migrations
- Payment webhook retry metadata
- Admin monitoring endpoints
- Operational summary endpoints under `/api/ops`
- Smoke/regression scripts under `scripts`
- Postman collections under `application-module/postman`

Interview points:

- Observability helps debug production issues without directly touching the database.
- Audit logs are important for admin actions and financial flows.
- Smoke tests and Postman collections support release validation.

## Important API Areas

Admin:

- `/api/admin/login`
- `/api/admin/verify-otp`
- `/api/admin/dashboard`
- `/api/admin/users`
- `/api/admin/bookings`
- `/api/v1/admin/payments`
- Admin service/category/banner/support/settings APIs

Customer:

- Customer auth/profile/address APIs
- Customer service listing
- Cart and add-on flows
- Coupon/referral flows
- Booking/subscription flows
- Review and support flows

Partner:

- Partner profile/onboarding APIs
- Partner order APIs
- Partner dispatch/live tracking APIs
- Partner notification APIs
- Partner support APIs
- Emergency SOS API

Payment:

- `/api/v1/payments/create-order`
- `/api/v1/payments/retry`
- `/api/v1/payments/verify`
- `/api/v1/webhooks/razorpay`

Health/Ops:

- `/status`
- `/health`
- `/health/liveness`
- `/health/readiness`
- `/api/ops/summary`
- `/api/ops/deprecations`

## Main Business Flows

Customer booking flow:

1. Customer authenticates and opens the app.
2. Customer browses catalog/services.
3. Customer selects address/location, service, add-ons and slot.
4. Booking/order is created with idempotency protection.
5. Payment order is created or payment is verified.
6. Dispatch searches for an eligible partner.
7. Partner accepts/handles job.
8. Notifications and timeline updates are sent.
9. Admin can monitor and intervene if needed.

Partner dispatch flow:

1. Partner profile and skills are configured.
2. Partner location/availability is updated.
3. Dispatch evaluates distance, pincode match, active jobs and rating.
4. Booking assignment is created.
5. Partner receives job/order updates.
6. Live tracking and state transitions continue until completion.

Payment flow:

1. Backend creates Razorpay order.
2. Customer completes payment on client side.
3. Backend verifies signature.
4. Webhook confirms captured/failed/refund state.
5. Payment transaction is persisted.
6. Booking state and admin dashboard reflect payment status.

Admin operations flow:

1. Admin logs in with OTP.
2. Admin views dashboard/metrics.
3. Admin manages catalog, bookings, customers, partners and payments.
4. Admin actions are audited.
5. Admin can handle support, emergency and reconciliation cases.

## Testing And Quality

The repository includes unit, integration and persistence tests across modules.

Quality tools:

- JUnit tests
- Maven Surefire/Failsafe
- JaCoCo coverage
- Checkstyle
- SpotBugs
- Postman/Newman regression scripts
- MySQL validation scripts
- Critical smoke GitHub workflows

Useful commands:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd verify
.\scripts\postman-regression.ps1 -BaseUrl http://localhost:8082
```

Interview points:

- Tests are split by module and by concern: controller, service, repository, entity and integration tests.
- Static analysis and formatting checks run through Maven lifecycle.
- Postman regression validates API behavior after backend starts.

## Interview Talking Points

Use these points when explaining the project:

- "This is a modular monolith: separate Maven modules and package boundaries, but one deployable Spring Boot runtime."
- "The main runtime is `application-module`; it aggregates auth, user, order, payment, partner, catalog and notification modules."
- "Database changes are controlled through Flyway, and Hibernate only validates schema."
- "Payment integration uses Razorpay with signature verification, webhook idempotency and admin reconciliation."
- "Dispatch uses partner availability, location freshness, skills, serviceability and scoring configuration."
- "Admin operations are separated from customer and partner APIs and are backed by audit/monitoring features."
- "The system is production-oriented: health checks, metrics, audit logs, request tracing, smoke tests and environment-based config are included."
- "Tests and coverage are handled across modules using Surefire/Failsafe and aggregate JaCoCo."

## 2-Minute Interview Explanation

Use this answer when interviewer asks: "Tell me about your project."

> This project is the backend for Lexho, a home-service booking platform. It supports customer app, partner app and admin panel features. Technically, it is a Java 17 and Spring Boot 3 based multi-module Maven project.
>
> Architecture-wise, it is a modular monolith. We have separate modules like auth-service, user-service, order-service, payment-service, partner-module, catalog-module, notification-module and admin-module, but they are packaged through one main runtime module called application-module. This keeps deployment simple while still keeping business logic separated by domain.
>
> The customer side supports service browsing, profile, address, cart, coupons, referrals, booking and subscription flows. The partner side supports onboarding, profile, availability, dispatch, live tracking, order handling, notifications, support and emergency SOS. The admin side supports dashboard, catalog management, booking management, customer and partner management, payment reconciliation, support, settings and audit/monitoring.
>
> For persistence, we use MySQL with Spring Data JPA, and schema changes are controlled using Flyway migrations. Hibernate is configured in validate mode so production schema is not changed automatically.
>
> Security is handled using Spring Security and JWT. Admin login has OTP, rate-limit and lockout configuration. Payment uses Razorpay integration with signature verification, webhook idempotency and refund/reconciliation support.
>
> For production readiness, the project has Actuator health and metrics, request/correlation IDs, audit logs, Postman regression scripts, smoke tests, JaCoCo coverage, Checkstyle and SpotBugs.
>
> My main understanding in this project is how to design backend modules around business domains, keep APIs secure, handle payment and booking lifecycle safely, and prepare backend services for production debugging and operations.

## Module-Wise Short Answers

Use these when interviewer asks: "What is the role of this module?"

### `application-module`

Short answer:

> `application-module` is the main runnable Spring Boot application. It combines all domain modules and exposes the actual REST APIs for customer app, partner app and admin panel.

Hindi/English:

> Ye main runtime module hai. Baaki modules business logic provide karte hain, aur `application-module` un sabko combine karke ek deployable backend service banata hai.

### `auth-service`

Short answer:

> `auth-service` handles internal auth token generation, refresh and revoke flows. It supports JWT-based stateless authentication.

Hindi/English:

> Is module ka kaam authentication tokens manage karna hai, jaise token generate karna, refresh karna aur revoke karna.

### `user-service`

Short answer:

> `user-service` handles user/customer management, admin user APIs, JWT security support and calling-related features like call sessions, call logs and realtime signalling.

Hindi/English:

> Isme user profile, admin user management, security context aur calling feature ka backend logic hai.

### `order-service`

Short answer:

> `order-service` handles booking/order lifecycle from admin operations point of view, including listing, stats, details, update, cancel and delete operations.

Hindi/English:

> Booking aur order lifecycle ka main business logic yaha manage hota hai. Admin booking ko monitor, update ya cancel kar sakta hai.

### `payment-service`

Short answer:

> `payment-service` integrates Razorpay. It creates payment orders, verifies payments, handles retries, processes webhooks, supports refunds and provides admin reconciliation APIs.

Hindi/English:

> Payment module Razorpay ke saath integrate hai. Ye payment create, verify, webhook process, refund aur reconciliation handle karta hai.

### `partner-module`

Short answer:

> `partner-module` manages partner profile, onboarding, service mapping, partner marketplace readiness and partner-related domain data.

Hindi/English:

> Partner app ke liye jo data chahiye, jaise onboarding, profile, services, availability aur dispatch readiness, uska base yaha hai.

### `catalog-module`

Short answer:

> `catalog-module` manages service catalog data like categories, services and subcategories that customers browse and admins manage.

Hindi/English:

> Customer app me jo services dikhti hain aur admin panel se jo catalog manage hota hai, uska data catalog module support karta hai.

### `notification-module`

Short answer:

> `notification-module` supports notification persistence, push tokens and delivery-related lifecycle for customer and partner notifications.

Hindi/English:

> Push notification, device token aur notification lifecycle ke liye ye module use hota hai.

### `admin-module`

Short answer:

> `admin-module` provides admin entity and repository foundation. The main admin APIs are exposed from `application-module`.

Hindi/English:

> Admin ka base persistence yaha hai, aur admin panel ke operational APIs main application module me expose hote hain.

### `common-lib`

Short answer:

> `common-lib` contains shared code used by multiple modules so duplicate common logic can be avoided.

Hindi/English:

> Jo classes ya utilities multiple modules me common hain, unhe `common-lib` me rakha gaya hai.

### `coverage-report`

Short answer:

> `coverage-report` aggregates JaCoCo test coverage across all Maven modules.

Hindi/English:

> Ye module poore multi-module project ka combined test coverage report generate karta hai.

## Quick Interview Revision

Remember these 10 points before interview:

1. Project type: Spring Boot modular monolith.
2. Main runtime: `application-module`.
3. Language/version: Java 17, Spring Boot 3.
4. Database: MySQL.
5. Schema migration: Flyway.
6. Security: Spring Security + JWT + admin OTP.
7. Payment: Razorpay with signature verification and webhook idempotency.
8. Business domains: customer, partner, admin, booking, dispatch, payment, catalog, notification.
9. Production features: Actuator, metrics, audit logs, request IDs, smoke/regression scripts.
10. Quality: JUnit, integration tests, JaCoCo, Checkstyle, SpotBugs.

## Possible Interview Questions And Answers

Q: Why multi-module Maven?

A: It separates ownership by business capability. Auth, user, order, payment, partner, catalog and notification code can evolve independently while still being packaged into one runtime service.

Q: Is this microservices or monolith?

A: It is closer to a modular monolith. It has module boundaries like microservices, but deployment is simplified as one Spring Boot application.

Q: How is database schema managed?

A: Flyway migrations are the source of truth. Hibernate runs in validate mode, so schema drift is caught instead of auto-changing production tables.

Q: How do you secure APIs?

A: JWT authentication, Spring Security filters, role/user context, request validation, admin OTP/rate-limit settings and external webhook signature verification.

Q: How do you prevent duplicate payment webhook processing?

A: Webhook events are persisted with event identifiers and idempotency logic so duplicate events can be safely ignored.

Q: How is production debugging supported?

A: Actuator endpoints, metrics, request/correlation IDs, audit logs, admin monitoring APIs, payment webhook status and regression scripts help diagnose issues.

Q: What would you improve next?

A: Depending on scale, I would consider clearer module API boundaries, stronger contract tests between modules, more event-driven processing for dispatch/payment, and production dashboards around SLA, assignment latency and payment failure rates.
