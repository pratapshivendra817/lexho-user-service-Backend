# Lexho Booking E2E Audit Report

Audit Start: 2026-10-04
Last Updated: 2026-10-04
Current Phase: Phase 8 complete; Phase 0/1 backfilled; Next Phase: Phase 9 - SCHEDULE CONFLICT

## 1. Executive Summary

Phase 0 and Phase 1 were initially missing as source-backed report sections. They were backfilled on 2026-10-04 by reading actual backend, customer app, and partner app source. Existing Phase 2 and Phase 3 findings were preserved.

## 2. Repository / Entry Points

### Phase 0 Scope

Repository discovery covered:

- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master`
- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile`
- Partner app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile`

Backend top-level modules discovered:

- `application-module`
- `auth-service`
- `catalog-module`
- `common-lib`
- `notification-module`
- `order-service`
- `partner-module`
- `payment-service`
- `user-service`
- `admin-module`

### Backend Entry Points

PASS:

Booking/customer API:

- File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`
- Class: `LexhoExperienceController`
- Endpoints: `POST /api/bookings/create`, `POST /api/v1/bookings/create`, `GET /api/bookings/history`, payment options/status/refund endpoints.
- Evidence: `createBooking` is mapped with `@PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})`. `bookingHistory` is present. Payment endpoints include `/api/payments/initiate`, `/api/payments/status/{bookingId}`, `/api/payments/refund`.

Order creation:

- File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\service\OrderService.java`
- Class: `OrderService`
- Method: `placeOrder`
- Evidence: creates `Order`, maps cart rows to `OrderItem`, applies coupon, saves order/addons, clears legacy or customer cart.

Order database:

- Entity: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\entity\Order.java`
- Entity: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\entity\OrderItem.java`
- Repository: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\repository\OrderRepository.java`
- Evidence: `Order` contains `paymentStatus`, `scheduledAt`, `bookingType`, and dispatch metadata such as `dispatchAddressId`; `OrderRepository#findByIdForUpdate` uses pessimistic write lock.

Payment:

- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\controller\PaymentController.java`
- Service: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`
- Listener: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\payment\InstantBookingPaymentDispatchListener.java`
- Evidence: `PaymentController#createOrder` requires `Idempotency-Key`; `PaymentService#createOrder` returns an existing transaction when the same idempotency key exists; `PaymentService` publishes `PaymentCapturedEvent` and `PaymentFailedEvent`; listener starts instant dispatch after commit.

Dispatch:

- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\controller\AdminDispatchController.java`
- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\controller\PartnerDispatchController.java`
- Service: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`
- Service: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\PartnerDispatchEligibilityService.java`
- Repositories: `PartnerShiftRepository`, `CurrentPartnerLocationRepository`, `PartnerServiceSkillRepository`, `BookingEventRepository`, `PincodeAdjacencyRepository`
- Evidence: `AdminDispatchController#dispatchOrder` calls `DispatchOrchestrationService#dispatchOrder`; `PartnerDispatchController` exposes shift start/end, availability, location and skills endpoints; dispatch service owns matching, request creation, retry, and payment-gated instant dispatch.

Partner requests:

- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\controller\PartnerRequestController.java`
- Service: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`
- Entity: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\entity\PartnerServiceRequestEntity.java`
- Repository: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\repository\PartnerServiceRequestRepository.java`
- Scheduler: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\scheduler\PartnerRequestExpiryScheduler.java`
- Evidence: endpoints include `GET /api/v1/partner/requests`, `PATCH /api/v1/partner/requests/{requestId}/accept`, and reject equivalents, with `/api/partner` alternate base path.

Partner lifecycle:

- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\controller\LexhoPartnerOrderController.java`
- Service: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`
- Entity: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\entity\PartnerOrder.java`
- Repository: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\repository\PartnerOrderRepository.java`
- Evidence: partner app lifecycle endpoints later map to accept/arrive/start/start verification/complete/complete verification flows through `PartnerOrderService`.

Subscription:

- Controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\controller\SubscriptionController.java`
- Admin controller: `AdminSubscriptionController`
- Service: `SubscriptionBookingService`
- Service: `SubscriptionAutoDispatchService`
- Service: `SubscriptionVisitOrderService`
- Listener: `SubscriptionPaymentListener`
- Scheduler: `SubscriptionAutoDispatchScheduler`
- Entities: `SubscriptionMaster`, `SubscriptionVisit`, `SubscriptionPartnerAssignment`, `SubscriptionVisitPartnerAssignment`
- Repositories: `SubscriptionMasterRepository`, `SubscriptionVisitRepository`, `SubscriptionFrequencyRepository`, `SubscriptionPartnerAssignmentRepository`, `SubscriptionVisitPartnerAssignmentRepository`
- Evidence: `SubscriptionController` exposes `/api/subscription/create`, `/api/subscription/bookings`, list/detail, pause/resume/cancel, visit skip/reschedule. `SubscriptionPaymentListener` calls `SubscriptionBookingService#activateByBookingId` on captured payment. `SubscriptionAutoDispatchScheduler` exists.

Admin booking management:

- File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\admin\controller\AdminBookingManagementController.java`
- Class: `AdminBookingManagementController`
- Service: `AdminBookingManagementService`
- Evidence: exposes booking list/detail/status/cancel/live tracking/navigation/payment reconciliation/payment status/payout endpoints under `/api/v1/admin/booking-management`.

Schedulers/listeners found relevant to booking lifecycle:

- `InstantBookingSearchTimeoutScheduler` - expires stuck instant searches.
- `PartnerRequestExpiryScheduler` - expires pending partner requests.
- `DispatchCleanupScheduler` - cleans stale shift/location state.
- `SubscriptionAutoDispatchScheduler` - scans subscription visits.
- `InstantBookingPaymentDispatchListener` - dispatches paid instant bookings.
- `SubscriptionPaymentListener` - activates paid subscriptions.

### Customer App Entry Points

PASS:

Key files:

- API: `src/api/bookingApi.ts`
- API: `src/api/paymentApi.ts`
- API: `src/api/subscriptionBookingApi.ts`
- API: `src/api/subscriptionApi.ts`
- API: `src/api/subscriptionVisitsApi.ts`
- Cart screen: `src/screens/cart/CartCrudScreen.tsx`
- Booking screens: `BookingHistoryScreen`, `BookingDetailScreen`, `InstantBookingStatusScreen`, `ScheduledBookingStatusScreen`, `BookingPaymentConfirmScreen`, `RecurringBookingScreen`, `SubscriptionPlanScreen`
- Hooks: `useInstantBookingLifecycle`, `useBookingLifecycleNavigation`
- Offline queue: `src/services/offlineQueue.ts`

Evidence:
`bookingApi.createBooking` posts to `/api/v1/bookings/create` with fallback `/api/bookings/create`. `CartCrudScreen` calls `createBooking` and routes instant bookings to payment while scheduled bookings route to scheduled status. `paymentApi.createPaymentOrder` posts `/api/payments/create-order` with `Idempotency-Key`. `subscriptionBookingApi.createSubscriptionBooking` posts `/api/subscription/create` and fallback `/api/subscription/bookings`.

Flow impact:
Customer frontend has entry points for instant, scheduled, subscription, payment, history, detail, and status screens.

### Partner App Entry Points

PASS:

Key files:

- API/service: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`
- Shift API: `src/features/shift/shiftApi.ts`
- Request screen: `src/screens/main/NewOrdersScreen.tsx`
- Active job screen: `src/screens/main\ActiveJobScreen.tsx`
- History screen: `src/screens/main\OrderHistoryScreen.tsx`
- Realtime hook: `src/hooks/useRealtimeDispatchRequests.ts`

Evidence:
`partnerService.ts` defines `getNewDispatchRequests`, `acceptDispatchRequest`, `rejectDispatchRequest`, `acceptPartnerOrder`, `markOrderArrived`, `startOrder`, `verifyOrderStartPin`, `requestOrderCompletePin`, `verifyOrderCompletePin`, and `completeOrder`. `NewOrdersScreen` calls accept/reject dispatch functions. `ActiveJobScreen` calls arrive/start verification APIs.

Flow impact:
Partner app has source entry points for new requests, accept/reject, arrival, start, PIN verification, completion, history, and realtime request refresh.

### Phase 0 Interim Verdict

PASS:

Repository discovery was completed against all three required codebases. Actual modules, controllers, services, repositories, entities, DTOs, schedulers, listeners, customer API files/screens, partner APIs/screens, payment handlers, and dispatch services were identified.

Unresolved:

- Some large admin and support areas were only mapped at entry-point level because detailed behavior belongs to later phases.
- Phase 0 did not prove every endpoint works; it only established source locations and audit entry points.

PHASE 0 COMPLETE

### Phase 1 - Common Booking Creation

Trace target:

```text
Customer Cart
-> createBooking()
-> POST /api/v1/bookings/create
-> fallback POST /api/bookings/create
-> LexhoExperienceController#createBooking
-> OrderService#placeOrder
-> OrderRepository / Order entity
-> payment initialization path
-> cart clearing
```

### Finding: Customer create booking API uses canonical endpoint plus fallback

PASS:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Function: `createBooking`

Evidence:
`createBooking` builds a body with `serviceId`, `addressId`, coordinates, `bookingType`, `date`, `slot`, `note`, and `couponCode`; it posts to `/api/v1/bookings/create`. It only falls back to `/api/bookings/create` on HTTP `404` or `405`. Offline/network errors enqueue the primary endpoint through `offlineQueue`.

Flow impact:
Both required common booking endpoints are represented in the customer app, with `/api/v1/bookings/create` as primary.

### Finding: Customer checkout has frontend duplicate submission guard

PARTIAL:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Component/method: `CartCrudScreen#handleCheckout`

Evidence:
`checkoutInFlightRef.current` is checked and set before checkout. The screen also tracks `checkoutInFlight`. The flag is reset in `finally` after a timeout.

Flow impact:
Duplicate taps in the same mounted UI are reduced. This is not server idempotency.

### Finding: Backend maps both create endpoints to the same controller method

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/method: `LexhoExperienceController#createBooking`  
Endpoint: `POST /api/bookings/create`, `POST /api/v1/bookings/create`

Evidence:
`createBooking` is annotated with `@PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})`. It rate-limits key `booking_create`, validates address, resolves booking type, validates booking type enablement, builds `PlaceOrderRequest`, calls `orderService.placeOrder`, calls `completeBookingDispatch`, returns `createdBookingPayload`, and writes audit.

Flow impact:
Primary and fallback frontend booking endpoints land on the same backend order-creation workflow.

### Finding: Booking create DTO validates core shape but has no idempotency key

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\dto\LexhoDtos.java`  
Record: `LexhoDtos.BookingCreateRequest`

Evidence:
`BookingCreateRequest` requires positive `serviceId`, nonblank `addressId`, constrains `bookingType` to `INSTANT|SCHEDULED`, and accepts `date`, `slot`, note, latitude/longitude aliases, coordinate pairs, `expectedTotal`, and `couponCode`. There is no idempotency-key field.

Flow impact:
Basic validation exists. Server-side duplicate booking protection cannot use a request idempotency key because the DTO does not carry one.

### Finding: Backend validates address ownership, geofence, and customer serviceability before order creation

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/methods: `LexhoExperienceController#validateBookingAddress`, `validateLegacyBookingAddress`, `validateCustomerLocationAddress`

Evidence:
The controller resolves UUID customer addresses or legacy numeric addresses. Customer-address path calls `findByIdAndCustomerIdAndIsDeletedFalse`, mirrors into `LexhoAddress`, validates geofence through `geoFenceValidationService.validateBookingAddress`, and calls `validateCustomerServiceability`.

Flow impact:
Booking creation is tied to customer-owned address data and location/serviceability checks before the order is created.

### Finding: Booking type resolution and schedule mapping are centralized

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/methods: `resolveBookingType`, `validateBookingTypeEnabled`, `placeOrderRequest`, `parseScheduledAt`

Evidence:
`resolveBookingType` defaults blank booking type to `SCHEDULED`; explicit request values are parsed into `BookingType`. `validateBookingTypeEnabled` checks cart config flags for instant/scheduled. `placeOrderRequest` sets `bookingType`, dispatch metadata, coupon, expected total, and `scheduledAt`: scheduled uses parsed date/slot; instant uses `LocalDateTime.now()`.

Flow impact:
Common booking creation handles both instant and scheduled using a single order path, with different post-create behavior.

### Finding: Order creation consumes cart, validates price, applies coupon, and clears cart

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\service\OrderService.java`  
Class/method: `OrderService#placeOrder`

Evidence:
`placeOrder` reads legacy cart rows with `cartRepository.findByUserId` or new customer cart rows with `customerCartItemRepository.findByUserId`. It creates `Order`, sets `OrderStatus.CREATED`, `PaymentStatus.PENDING`, address, scheduled time, booking type and dispatch metadata. It validates cart price against `expectedTotal` if provided, applies coupon through `couponService.applyToOrder`, saves order and addons, then deletes cart rows or customer cart rows plus cart-item addons.

Flow impact:
The cart-to-order handoff is connected and cart clearing happens after order creation.

### Finding: Order entity/repository support booking lifecycle metadata and row locking

PASS:

Entity file: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\entity\Order.java`  
Repository file: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\repository\OrderRepository.java`

Evidence:
`Order` includes `paymentStatus`, `scheduledAt`, `bookingType`, and dispatch fields including `dispatchAddressId`. `OrderRepository#findByIdForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

Flow impact:
The common booking row has the core fields needed by payment, dispatch, scheduled waiting, and later lifecycle updates.

### Finding: Post-create handling splits instant and scheduled

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/methods: `completeBookingDispatch`, `markInstantDispatchPending`, `markScheduledBooking`

Evidence:
`completeBookingDispatch` sends instant bookings to `markInstantDispatchPending` and scheduled bookings to `markScheduledBooking`. Instant path locks the order, applies booking type if missing, transitions to `SEARCHING_PARTNER` when allowed, sets SLA minutes, and records `INSTANT_DISPATCH_PENDING`. Scheduled path locks the order and transitions `CREATED` to `SCHEDULED` when allowed.

Flow impact:
Common creation is shared, but post-create lifecycle diverges immediately by booking type.

### Finding: Customer instant route locks cart until payment; scheduled route refreshes cart/status

PASS:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Component/method: `CartCrudScreen#handleCheckout`

Evidence:
After create, instant bookings dispatch `lockCartUntilPayment({ bookingId })` and navigate to `Payment`. Scheduled bookings dispatch `fetchCustomerCartThunk()` and navigate to `ScheduledBookingStatus`.

Flow impact:
Customer app honors different post-create UX for instant payment-first flow and scheduled waiting/status flow.

### Finding: Legacy payment initiate endpoint sets booking payment pending but does not create Razorpay order

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/method: `LexhoExperienceController#initiatePayment`  
Endpoint: `POST /api/payments/initiate`

Evidence:
The endpoint resolves booking ID, verifies user ownership, locks the order, sets `PaymentStatus.PENDING`, and returns amount/status plus local Razorpay option metadata for UPI/CARD. It does not call `PaymentService#createOrder`.

Flow impact:
This endpoint is a legacy/local payment initiation surface. The real Razorpay order path is separate.

### Finding: Real payment order creation is idempotency-key protected

PASS:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\paymentApi.ts`  
Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\payment\PaymentScreen.tsx`  
Backend controller: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\controller\PaymentController.java`  
Backend service: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`

Evidence:
`PaymentScreen#buildIdempotencyKey` creates a booking-scoped key and passes it to `paymentApi.createPaymentOrder`. `paymentApi.createPaymentOrder` sends header `Idempotency-Key` to `/api/payments/create-order`. `PaymentController#createOrder` requires that header, and `PaymentService#createOrder` returns an existing transaction when `transactionRepository.findByIdempotencyKey(idempotencyKey)` finds one.

Flow impact:
Payment order creation has idempotency protection even though booking creation does not.

### Finding: Booking creation lacks server idempotency

IDEMPOTENCY RISK:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Backend DTO: `LexhoDtos.BookingCreateRequest`

Evidence:
`bookingApi.createBooking` does not send an idempotency header or key in the body. `BookingCreateRequest` has no idempotency field. `LexhoExperienceController#createBooking` rate-limits but does not check a request idempotency key before calling `orderService.placeOrder`.

Flow impact:
Offline replay, app retry, or duplicate submit outside the in-memory UI guard can create duplicate orders from the same cart intent. This risk was also observed in Phase 2 and remains valid.

### Finding: Common booking create returns a normalized payload

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/methods: `createdBookingPayload`, `baseCreatedBookingPayload`

Evidence:
The payload includes `id`, `bookingId`, contract version, `serviceId`, address id, booking type, assignment status, date, slot, note, order status, SLA minutes, meta, tracking payload, and status history.

Flow impact:
The customer app can route and render immediate post-create status without separately loading detail first.

### Phase 1 Interim Verdict

PARTIAL:

Verified connected:
- Customer `createBooking` primary and fallback endpoints.
- Backend shared create controller for `/api/v1/bookings/create` and `/api/bookings/create`.
- DTO validation for required booking fields.
- Address ownership, geofence, and serviceability validation.
- Cart to order conversion.
- Expected total validation.
- Coupon application.
- Cart clearing after order save.
- Instant/scheduled post-create lifecycle split.
- Real payment order creation with idempotency key.

Risks:
- Booking create has no server idempotency key. Classification: IDEMPOTENCY RISK.
- Frontend duplicate submission guard is UI-only. Classification: PARTIAL.
- Legacy `/api/payments/initiate` is not the real Razorpay order creation path. Classification: PARTIAL.

PHASE 1 COMPLETE

## 3. Instant Booking Audit

### Phase 2 Scope

Flow audited in this phase:

Customer Cart -> `bookingMode = INSTANT` -> `createBooking()` -> backend booking creation -> order/payment pending -> payment create/verify -> payment captured listener -> dispatch -> partner request -> partner accept -> arrive -> start -> complete -> customer history.

### Finding: Customer UI selects instant booking and builds instant schedule window

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Class/Component: `CartCrudScreen`  
Method/Function: `selectBookingMode`, `handleCheckout`, `getInstantIsoTime`

Evidence:
`BookingMode` includes `INSTANT`, and `selectBookingMode("INSTANT")` sets `requiresScheduledSlotSelection(false)`, calculates `getInstantIsoTime(cartPolicy.instantBookingWindowMinutes)`, stores it via `setScheduledTime`, and updates cart metadata if an address is available. In `handleCheckout`, if `bookingMode === "INSTANT"` and no scheduled time exists, the same instant ISO fallback is created before booking.

Flow impact:
Customer instant checkout has a frontend path that sets a near-term slot/window even though the customer selected instant. This is later mapped into `date`/`slot` for the backend create request.

### Finding: Customer duplicate submission is guarded in UI only

PARTIAL:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Class/Component: `CartCrudScreen`  
Method/Function: `handleCheckout`

Evidence:
`handleCheckout` starts with `if (checkoutInFlightRef.current) return;`, sets `checkoutInFlightRef.current = true`, sets `checkoutInFlight(true)`, and disables the primary button while `checkoutInFlight` is true. It resets the flag in `finally` after a 1200 ms timeout.

Flow impact:
Duplicate taps in the same mounted UI are reduced. This is frontend-only protection; Phase 2 did not find a booking-create idempotency key sent by `createBooking()`.

### Finding: Customer booking API uses primary and legacy create endpoints

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Function: `createBooking`

Evidence:
`createBooking()` posts to `/api/v1/bookings/create`. It only tries `/api/bookings/create` when the primary error status is `404` or `405`. The request body includes `serviceId`, `addressId`, `latitude`, `longitude`, `coordinates`, `bookingType`, `date`, `slot`, `note`, and `couponCode`.

Flow impact:
The customer app primary and fallback instant booking endpoints are both present in frontend code. Offline/network failures enqueue the primary endpoint in `offlineQueue`, which creates a retry path but not a server-side idempotency guarantee.

### Finding: Instant booking is routed to payment after creation

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Class/Component: `CartCrudScreen`  
Method/Function: `handleCheckout`

Evidence:
After `createBooking({ bookingType: "INSTANT", ... })`, if a `bookingId` is returned, the UI dispatches `lockCartUntilPayment({ bookingId })` and navigates to `Payment` with `bookingMode: "INSTANT"` and `postPaymentRoute: "BookingPaymentConfirm"`.

Flow impact:
Instant bookings are not shown as immediately active in the customer flow; they are intentionally sent through payment first.

### Finding: Backend maps both create endpoints to one controller method

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class: `LexhoExperienceController`  
Method: `createBooking`  
Endpoint: `POST /api/bookings/create`, `POST /api/v1/bookings/create`

Evidence:
The method is annotated with `@PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})`. It rate-limits key `booking_create`, validates address, resolves `BookingType`, validates booking type enablement, creates a `PlaceOrderRequest`, calls `orderService.placeOrder(...)`, then calls `completeBookingDispatch(...)`, returns created booking payload, and writes audit.

Flow impact:
Primary and fallback customer app endpoints land on the same backend method.

### Finding: Backend creates order from cart and clears cart

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\service\OrderService.java`  
Class: `OrderService`  
Method: `placeOrder`

Evidence:
`placeOrder()` reads legacy `CartRepository.findByUserId(userId)` or `CustomerCartItemRepository.findByUserId(userId)`, creates `Order`, sets `status = CREATED`, `paymentStatus = PENDING`, `bookingType`, schedule and dispatch metadata. It converts cart rows to `OrderItem`s, applies coupon with `couponService.applyToOrder(...)`, saves the order, saves order add-ons, then deletes either legacy cart rows or customer cart rows plus cart item add-ons.

Flow impact:
Instant order creation is connected to cart consumption and cart clearing. This is the authoritative customer order creation path for Phase 2.

### Finding: Instant dispatch is payment-gated at dispatch orchestration

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `dispatchOrderInternal`, `shouldWaitForPayment`

Evidence:
`dispatchOrderInternal(...)` builds dispatch context, calls `ensureServiceable(...)`, saves `BOOKING_CREATED`, then checks `shouldWaitForPayment(context)`. `shouldWaitForPayment` returns true when `context.instant()` and `order.paymentStatus != PaymentStatus.SUCCESS`. In that case it saves `DISPATCH_PAYMENT_PENDING`, publishes `PAYMENT_PENDING`, and returns `0`.

Flow impact:
Instant order creation does not create partner requests until payment status is `SUCCESS`. This satisfies the Phase 2 critical payment-before-dispatch requirement for the normal orchestration path.

### Finding: Payment create order is idempotency-key protected, but booking create is not

PARTIAL:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\payment\PaymentScreen.tsx`  
Customer function: `buildIdempotencyKey`, payment mutation  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\controller\PaymentController.java`  
Backend class/method: `PaymentController#createOrder`  
Backend service: `PaymentService#createOrder`

Evidence:
`PaymentScreen` creates `booking-${bookingId}-${uuid|Date.now}` and passes it to `createPaymentOrder`. `paymentApi.ts#createPaymentOrder` sends header `Idempotency-Key`. Backend `PaymentController#createOrder` requires `@RequestHeader("Idempotency-Key")`, and `PaymentService#createOrder` returns an existing transaction when `transactionRepository.findByIdempotencyKey(idempotencyKey)` finds one.

Flow impact:
Payment order creation is protected from exact idempotency-key retries. Booking creation itself has no equivalent key in `bookingApi.ts#createBooking` or `LexhoExperienceController#createBooking`, so customer offline retry / double-submit outside the in-memory UI guard remains an idempotency risk for booking creation.

Classification for booking create: IDEMPOTENCY RISK.

### Finding: Payment success updates order and emits captured event

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Method: `verify`, `markPaid`

Evidence:
`verify(...)` validates Razorpay signature, fetches payment, validates amount/order, then calls `markPaid(...)` when captured. `markPaid(...)` sets transaction `PAID`, locks booking via `orderRepository.findByIdForUpdate`, sets booking `paymentStatus = SUCCESS`, transitions `CREATED` to `CONFIRMED` through `OrderLifecycleService`, and publishes `PaymentCapturedEvent` when newly paid.

Flow impact:
Successful Razorpay verification updates the order and creates the event that starts instant dispatch after commit.

### Finding: Payment captured listener starts instant dispatch after commit

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\payment\InstantBookingPaymentDispatchListener.java`  
Class: `InstantBookingPaymentDispatchListener`  
Method: `onPaymentCaptured`

Evidence:
`onPaymentCaptured(PaymentCapturedEvent event)` is annotated with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` and calls `dispatchOrchestrationService.dispatchPaidInstantBooking(event.bookingId())`.

Flow impact:
Instant dispatch is connected to payment success, and it runs after the payment transaction commits.

### Finding: Paid instant dispatch has duplicate-start guard

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `dispatchPaidInstantBooking`

Evidence:
The method locks the order with `findByIdForUpdate`, returns `0` unless booking type is `INSTANT`, returns `0` when payment is not `SUCCESS`, and returns `0` when `dispatchStartedAt != null`, `assignedPartnerId != null`, or status is `PARTNER_ASSIGNED` / `PARTNER_ACCEPTED`. It sets `dispatchStartedAt` before calling `dispatchOrderInternal(...)`.

Flow impact:
Duplicate payment captured events should not start duplicate dispatch for the same paid instant order once `dispatchStartedAt` is set.

### Finding: Dispatch creates partner request records from eligible partners

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `dispatchAttemptsForOrder`, `findEligibleCandidates`, `createPartnerRequests`, `toPartnerServiceRequest`
Entity: `PartnerServiceRequestEntity`
Repository: `PartnerServiceRequestRepository`

Evidence:
`dispatchAttemptsForOrder(...)` finds existing requests by `cartId` (order id), excludes already requested partner ids, selects initial candidates within search radius, expands radius when none are found, and creates `PartnerServiceRequestEntity` rows. The entity stores `partnerId`, `cartId`, `customerId`, `serviceId`, `scheduledTime`, full address, coordinates, `targetPincode`, `dispatchAttempt`, `distanceKm`, `etaMinutes`, `expiresAt`, and `status = PENDING`.

Flow impact:
Partner matching creates explicit partner request rows rather than directly creating partner orders for instant booking.

### Finding: Dispatch validates serviceability before request creation

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `ensureServiceable`
Repository: `ServiceableAreaRepository`

Evidence:
`ensureServiceable(...)` requires `serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse(context.pincode())`; otherwise it saves `PARTNER_SEARCH_FAILED` with `PINCODE_NOT_SERVICEABLE` and throws `BadRequestException("Service is not available for this pincode")`.

Flow impact:
Instant dispatch does not proceed outside active serviceable pincodes.

### Finding: Dispatch skill matching exists for instant candidates

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `hasRequiredSkill`, `isEligiblePartner`, `candidateForShift`
Repository: `PartnerServiceSkillRepository`

Evidence:
`hasRequiredSkill(...)` returns true when partner `subCategoryId` equals requested sub-service, or partner `categoryId` equals requested category, or `PartnerServiceSkillRepository` has partner/sub-service or partner/category skill rows. `candidateForShift(...)` calls `isEligiblePartner(...)` before distance and ETA checks.

Flow impact:
Automatic instant dispatch checks partner skill/category match before request creation.

### Finding: Partner request list and accept/reject endpoints are connected

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\controller\PartnerRequestController.java`  
Class: `PartnerRequestController`  
Endpoints: `GET /api/v1/partner/requests`, `GET /api/partner/requests`, `PATCH /api/v1/partner/requests/{requestId}/accept`, `PATCH /api/partner/requests/{requestId}/accept`, `PATCH /api/v1/partner/requests/{requestId}/reject`, `PATCH /api/partner/requests/{requestId}/reject`

Evidence:
Controller-level mapping is `@RequestMapping({"/api/partner", "/api/v1/partner"})`. `requests(...)` delegates to `PartnerRequestService#getIncomingRequests`. `accept(...)` rate-limits key `partner_accept` then calls `PartnerRequestService#accept`. `reject(...)` rate-limits key `partner_reject` then calls `PartnerRequestService#reject`.

Flow impact:
Partner app’s preferred request endpoints are backed by backend endpoints.

### Finding: Partner app tries request endpoints before legacy order accept

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`  
Functions: `getNewDispatchRequests`, `acceptDispatchRequest`, `rejectDispatchRequest`

Evidence:
`getNewDispatchRequests` tries `/api/v1/partner/requests`, `/api/v1/orders/new`, `/api/partner/requests`, `/api/orders/new`, `/api/partner/orders/new`, `/api/partner/dispatch/new`, `/api/partner/dispatch/requests/new`. `acceptDispatchRequest` tries `PATCH /api/v1/partner/requests/{id}/accept` and `/api/partner/requests/{id}/accept` before subscription and legacy `/api/v1/orders/{id}/accept` / `/api/orders/{id}/accept` fallbacks.

Flow impact:
The partner app can accept real dispatch requests, not only direct open orders.

### Finding: Partner accept locks request and order but peer request rejection is not locked as a set

CONCURRENCY RISK:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`
Repository: `PartnerServiceRequestRepository`

Evidence:
The accepted request is locked by `findByIdAndPartnerIdForUpdate`. The order is locked by `orderRepository.findByIdForUpdate(request.getCartId())`. After setting the accepted request status, the method finds peer pending requests through `findByCartIdAndStatus(...)` without a pessimistic lock and marks them `REJECTED`.

Flow impact:
The locked order prevents two partners from assigning the same instant order if both calls reach `DispatchOrchestrationService#markPartnerAccepted`, but peer request rows are not locked as a group. Concurrent accept attempts can race through request status checks; the order lock is the main protection. Keep this as a concurrency risk to revisit in Phase 14.

### Finding: Partner accept enforces paid instant order

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`

Evidence:
For non-subscription requests, the method loads and locks the order. If `instant && order.getPaymentStatus() != PaymentStatus.SUCCESS`, it marks the request `REJECTED` with `PAYMENT_FAILED` or `PAYMENT_PENDING` and throws `BadRequestException("Order payment is not confirmed")`.

Flow impact:
Even if a partner request exists unexpectedly before payment, partner accept is blocked for unpaid instant orders.

### Finding: Partner assignment updates order status and active job count

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `markPartnerAccepted`

Evidence:
The method locks the order, rejects if assigned to another partner, locks partner shift, checks `PartnerDispatchEligibilityService.canReceiveNewBookings`, applies status transition to `PARTNER_ASSIGNED` for instant bookings, sets SLA/promised start time, clears unavailable reason, sets `assignedPartnerId`, saves the order, increments shift `activeJobsCount`, saves booking event `PARTNER_ACCEPTED`, and publishes lifecycle event.

Flow impact:
Instant request acceptance writes the authoritative customer order assignment.

### Finding: Legacy partner order accept is not the right instant accept path

IMPLEMENTED BUT NOT CONNECTED:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Method: `transition`, called by `acceptOrder`

Evidence:
For `to == ACCEPTED`, the method checks `boolean instant = "INSTANT".equalsIgnoreCase(order.getBookingType())`; if instant and `assignedPartnerId == null`, it throws `BadRequestException("Instant orders must be accepted through partner requests")`. If instant and payment is not `SUCCESS`, it also throws.

Flow impact:
The direct `/api/v1/orders/{id}/accept` fallback cannot accept a newly dispatched instant order unless it is already assigned. This is correct for request-based instant dispatch but means partner frontend fallbacks are not equivalent.

### Finding: Arrival/start/complete lifecycle endpoints exist

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\controller\LexhoPartnerOrderController.java`  
Class: `LexhoPartnerOrderController`  
Methods/endpoints:
`arrive` -> `POST /api/v1/orders/{id}/arrive`, `POST /api/orders/{id}/arrive`  
`start` -> `POST /api/v1/orders/{id}/start`, `POST /api/orders/{id}/start`  
`verifyStartPin` -> `POST /api/v1/orders/{id}/start/verify`, `POST /api/orders/{id}/start/verify`  
`requestCompletePin` -> `POST /api/v1/orders/{id}/complete-pin`, `POST /api/orders/{id}/complete-pin`  
`complete` -> `POST /api/v1/orders/{id}/complete`, `POST /api/orders/{id}/complete`  
`verifyCompletePin` -> `POST /api/v1/orders/{id}/complete/verify`, `POST /api/orders/{id}/complete/verify`

Evidence:
The controller maps `@RequestMapping({"/api/orders", "/api/v1/orders"})` and delegates each lifecycle method to `PartnerOrderService`.

Flow impact:
Backend has partner-facing arrival/start/complete endpoints for the partner app’s lifecycle actions.

### Finding: Partner order service lifecycle expects partner-order status enum, while instant dispatch writes common order enum statuses

UNKNOWN — CODE EVIDENCE INSUFFICIENT:

Files:
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\entity\OrderStatus.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\common-lib\src\main\java\com\lexho\common\enums\OrderStatus.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`

Evidence:
Instant dispatch sets common order status `PARTNER_ASSIGNED` in `DispatchOrchestrationService#markPartnerAccepted`. `PartnerOrderService` uses `com.lexho.user_service.lexhopartner.entity.OrderStatus`, whose enum contains `CREATED`, `ASSIGNED`, `ACCEPTED`, `ARRIVED`, `STARTED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELLED` and does not include `PARTNER_ASSIGNED`. `PartnerOrderRepository` uses native query statuses including `PARTNER_ASSIGNED`, but JPA mapping into the partner enum for the same `orders.status` column may be fragile. This needs entity column converter verification in later state-machine phase.

Flow impact:
Arrival/start/complete may fail to load or transition an instant order whose DB status is `PARTNER_ASSIGNED` unless there is a converter or mapping not yet verified. Phase 2 cannot mark the partner lifecycle fully connected until this enum boundary is audited in Phase 11.

### Finding: Customer start and completion PINs are generated only after partner lifecycle states

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Methods: `arriveOrder`, `startOrder`, `requestCompletePin`, `completeOrder`, `verifyPin`

Evidence:
`arriveOrder(...)` allows `ACCEPTED`/`ASSIGNED`, calls `ensureStartPin(...)`, then saves transition to `ARRIVED`. `startOrder(...)` allows only `ARRIVED`, verifies start PIN, optionally stores before image, then saves transition to `IN_PROGRESS`. `requestCompletePin(...)` allows only `IN_PROGRESS`, generates complete PIN. `completeOrder(...)` requires `IN_PROGRESS`, requires start PIN consumed, verifies complete PIN, optionally stores after image, and saves `COMPLETED`.

Flow impact:
Partner start and completion are protected by customer PIN verification in the partner order lifecycle.

### Finding: Payment failure cancels instant booking and expires pending requests

PASS:

Files:
`C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\payment\InstantBookingPaymentDispatchListener.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`

Evidence:
`PaymentService#markFailed` sets transaction `FAILED`, sets booking `paymentStatus = FAILED`, publishes `PaymentFailedEvent`. `InstantBookingPaymentDispatchListener#onPaymentFailed` calls `DispatchOrchestrationService#handlePaymentFailed`. That method expires pending partner requests, skips terminal/in-progress statuses, decrements active jobs if assigned, clears `assignedPartnerId`, transitions to `CANCELLED`, sets `unavailableReason = PAYMENT_FAILED`, and publishes `PAYMENT_FAILED`.

Flow impact:
Payment failure is connected to instant booking cleanup.

### Finding: No-partner search timeout exists

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\InstantBookingSearchTimeoutScheduler.java`  
Class: `InstantBookingSearchTimeoutScheduler`  
Method: `expireStuckSearches`

Evidence:
Scheduler runs with `@Scheduled(fixedDelayString = "${instant.booking.search-timeout-scan-ms:60000}")`, computes cutoff from `${instant.booking.search-timeout-seconds:180}`, and calls `dispatchOrchestrationService.expireStuckInstantSearches(cutoff)`. Dispatch service marks qualifying paid, unassigned `SEARCHING_PARTNER` instant orders as `INSTANT_FAILED`, expires pending requests, writes `SEARCH_TIMEOUT`, and publishes lifecycle event.

Flow impact:
Instant bookings should not remain forever in `SEARCHING_PARTNER` when no partner accepts, provided scheduling is enabled.

### Finding: Partner rejection triggers retry/continued dispatch

PARTIAL:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `reject`

Evidence:
`reject(...)` marks request `REJECTED`, calls `dispatchOrchestrationService.markPartnerRejected(request.getCartId(), partnerId)`, then `maybeContinueDispatch(request.getCartId())`. The exact conditions inside `maybeContinueDispatch` need further audit in error-handling and idempotency phases.

Flow impact:
Rejection is connected to retry hooks, but complete retry behavior and backoff are not fully classified in Phase 2.

### Finding: Customer history separates instant bookings

PASS:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Function: `getBookingsHistoryPage`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class/method: `LexhoExperienceController#bookingHistory`

Evidence:
Customer app calls `GET /api/bookings/history` and maps `instantBookings`, `scheduledBookings`, and `subscriptions`. Backend `bookingHistory(...)` filters `orders` into `instantBookings` when `bookingType == INSTANT`, `scheduledBookings` when scheduled/null/blank, and `subscriptions` when subscription type.

Flow impact:
Instant booking history is connected from backend grouping to customer API mapper.

### Phase 2 Interim Verdict

PARTIAL:

Verified connected:
- Customer instant mode selection and instant checkout.
- `/api/v1/bookings/create` with `/api/bookings/create` fallback.
- Backend order creation from cart with cart clearing.
- Payment order creation with idempotency key.
- Payment success -> order `PaymentStatus.SUCCESS` -> `PaymentCapturedEvent`.
- `PaymentCapturedEvent` -> paid instant dispatch after commit.
- Dispatch payment gate blocks unpaid instant partner requests.
- Partner request creation with skill/serviceability/distance/ETA checks.
- Partner request accept path assigns order through `DispatchOrchestrationService#markPartnerAccepted`.
- Payment failure cleanup cancels instant bookings before service starts.
- No-partner timeout exists.

Not fully verified / risks:
- Booking creation lacks server idempotency key. Classification: IDEMPOTENCY RISK.
- Partner request peer rejection is not locked as a set. Classification: CONCURRENCY RISK.
- Partner lifecycle after assignment has a possible enum/status boundary mismatch between common order statuses (`PARTNER_ASSIGNED`) and partner order statuses (`ASSIGNED`). Classification: UNKNOWN — CODE EVIDENCE INSUFFICIENT until Phase 11.
- Partner rejection retry behavior is only partially verified in Phase 2; continue in Phase 17.

PHASE 2 COMPLETE

## 4. Scheduled Booking Audit

### Phase 3 Scope

Trace target:

```text
Customer
-> Scheduled Date
-> Scheduled Slot
-> Booking
-> Payment
-> Waiting
-> Dispatch Window
-> Partner Matching
-> Partner Request
-> Accept
-> Arrive
-> Start
-> Complete
-> History
```

Inspected source:

- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`
- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`
- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\components\cart\TimePicker.tsx`
- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\booking\ScheduledBookingStatusScreen.tsx`
- Partner app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\service\OrderService.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\config\DispatchProperties.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\controller\PartnerRequestController.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\PartnerDispatchEligibilityService.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\repository\PartnerShiftRepository.java`
- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master\order-service\src\main\java\com\lexho\booking\repository\OrderRepository.java`

Classes requested by Phase 3:

- `DispatchOrchestrationService`: exists.
- `ScheduledBookingDispatchScheduler`: not found in `application-module/src/main/java` by source filename or scheduler scan.
- `SchedulingConflictService`: not found in `application-module/src/main/java` by source filename or conflict search.

### Finding: Customer scheduled slot selection validates future time

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\components\cart\TimePicker.tsx`  
Component/method: `TimePicker#handleConfirm`

Evidence:
`handleConfirm` builds `new Date(`${selectedDate}T${selectedSlot}:00`)` and rejects invalid or past/equal times by calling `onChange(null)` when `scheduled.getTime() <= Date.now()`.

Flow impact:
The customer UI prevents confirming a past scheduled slot before checkout.

### Finding: Customer scheduled booking payload is connected to create booking API

PASS:

Customer file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Function: checkout/create booking block around `createBooking(...)`  
API file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Function: `createBooking`

Evidence:
`CartCrudScreen` sends `bookingType: bookingMode === "INSTANT" ? "INSTANT" : "SCHEDULED"`, `serviceId`, `addressId`, coordinates, `date`, `slot`, note, and coupon to `createBooking(...)`. `bookingApi.createBooking` posts the normalized body to `POST /api/v1/bookings/create` and falls back to `POST /api/bookings/create` only on `404` or `405`.

Flow impact:
Scheduled booking request creation is connected from cart UI to both primary and fallback backend endpoints.

### Finding: Scheduled booking does not navigate to payment in current cart path

PARTIAL:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Function: checkout/create booking block

Evidence:
After successful creation, instant bookings call `dispatch(lockCartUntilPayment(...))` and navigate to `Payment`. Scheduled bookings call `dispatch(fetchCustomerCartThunk())` and navigate directly to `ScheduledBookingStatus` with `bookingId`, `serviceName`, `address`, amount, currency, and `scheduledAt`.

Flow impact:
Scheduled creation is connected, but the customer scheduled path is not payment-first in this source path. Phase 12 must decide whether scheduled bookings are allowed to remain unpaid before dispatch/start.

### Finding: Backend resolves and validates scheduled booking creation

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class: `LexhoExperienceController`  
Methods: `createBooking`, `resolveBookingType`, `validateBookingTypeEnabled`, `placeOrderRequest`, `validateBookingAddress`

Evidence:
`createBooking` rate-limits booking creation, validates the address, resolves the booking type, checks cart config enablement, builds `PlaceOrderRequest`, calls `orderService.placeOrder(...)`, and then calls `completeBookingDispatch(...)`. For scheduled bookings, `placeOrderRequest` sets `scheduledAt` with `parseScheduledAt(request.date(), request.slot())`; the address validation path checks customer-owned address/legacy address, geo-fence validation, and customer serviceability.

Flow impact:
Scheduled date/slot, address ownership, geofence, serviceability, service ID, coupon code, and booking type flow into backend order creation.

### Finding: Backend scheduled order persistence is connected

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\service\OrderService.java`  
Class: `OrderService`  
Method: `placeOrder`

Evidence:
`placeOrder` creates an `Order`, sets `userId`, status `CREATED`, payment status `PENDING`, address, `scheduledAt`, `bookingType`, dispatch address/service/quantity metadata, validates cart items and service availability, applies coupon, saves the order, saves addons, sends a notification best-effort, clears cart data, and returns `OrderResponseDTO`.

Flow impact:
Scheduled bookings are persisted as normal orders with scheduled timestamp and dispatch metadata needed for later dispatch.

### Finding: Scheduled booking is marked waiting instead of dispatched at create time

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class: `LexhoExperienceController`  
Methods: `completeBookingDispatch`, `markScheduledBooking`

Evidence:
`completeBookingDispatch` sends instant bookings to `markInstantDispatchPending(...)`; for non-instant it calls `markScheduledBooking(...)`. `markScheduledBooking` locks the order using `orderRepository.findByIdForUpdate(orderId)`, applies booking type if missing, transitions `CREATED` to `SCHEDULED` when allowed, and saves the order.

Flow impact:
Scheduled bookings enter a waiting state after creation. They are not partner-dispatched immediately by the customer create endpoint.

### Finding: No scheduled booking auto-dispatch scheduler found

IMPLEMENTED BUT NOT CONNECTED:

Backend files searched:
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java`

Evidence:
Scheduler scan found `DispatchCleanupScheduler`, `InstantBookingSearchTimeoutScheduler`, `PartnerRequestExpiryScheduler`, notification schedulers, and `SubscriptionAutoDispatchScheduler`, but no `ScheduledBookingDispatchScheduler` or scheduler that scans `Order` records with `bookingType = SCHEDULED` and due `scheduledAt`. `OrderRepository` has `countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(...)`, but no source path in Phase 3 uses it to dispatch due scheduled orders.

Flow impact:
Scheduled bookings can be created and marked `SCHEDULED`, but automatic transition from waiting to dispatch window is not connected by a scheduler in the inspected source.

### Finding: Scheduled dispatch window logic exists when dispatch is manually invoked

PARTIAL:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `dispatchOrder`, `dispatchOrderInternal`, `shouldDeferDispatch`, `shouldActivateScheduledDispatch`, `getScheduledPreDispatchMinutes`  
Config file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\config\DispatchProperties.java`

Evidence:
`dispatchOrder(...)` calls `dispatchOrderInternal(...)`. For scheduled orders, `shouldDeferDispatch(...)` returns true until `shouldActivateScheduledDispatch(scheduledAt)` is true. `shouldActivateScheduledDispatch` activates at `scheduledAt.minusMinutes(getScheduledPreDispatchMinutes())`. `DispatchProperties.Booking#preDispatchMinutes` defaults to `30`, clamped by `getScheduledPreDispatchMinutes()` to `5..240`.

Flow impact:
The dispatch window rule exists and defaults to 30 minutes before scheduled time, but it only runs when some caller invokes dispatch. Phase 3 did not find an automatic scheduled-order caller.

### Finding: Admin can manually dispatch scheduled orders

BACKEND-ONLY / FRONTEND GAP:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\controller\AdminDispatchController.java`  
Class: `AdminDispatchController`  
Endpoint: `POST /api/admin/dispatch/orders/{orderId}`, `POST /api/v1/admin/dispatch/orders/{orderId}`  
Method: `dispatchOrder`

Evidence:
Admin dispatch loads the order and calls `dispatchOrchestrationService.dispatchOrder(orderId, order.getUserId(), request.addressId, request.serviceId, request.quantity, order.getScheduledAt())`.

Flow impact:
Scheduled dispatch can be triggered manually/admin-side and will honor the pre-dispatch window. No customer app or automatic scheduled scheduler connection was found for this Phase 3 path.

### Finding: Scheduled dispatch does not enforce successful payment

FAIL:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `shouldWaitForPayment`, `dispatchOrderInternal`

Evidence:
`shouldWaitForPayment` returns `context.instant() && context.order().getPaymentStatus() != PaymentStatus.SUCCESS`. Because the condition is instant-only, scheduled dispatch does not wait for `PaymentStatus.SUCCESS`. `PartnerRequestService#accept` also enforces payment only when `instant` is true.

Flow impact:
If admin/manual dispatch is invoked for an unpaid scheduled booking inside the dispatch window, backend code can create partner requests and allow acceptance despite payment status `PENDING`. This violates a strict payment-before-dispatch rule for scheduled bookings unless product requirements explicitly allow pay-later scheduled service.

### Finding: Scheduled partner matching uses serviceability, distance, shift, availability, active-job and skill checks

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `ensureServiceable`, `findEligibleCandidates`, `candidateForShift`, `isEligiblePartner`, `isDispatchable`, `hasRequiredSkill`, `revalidateCandidateForNewDispatch`

Evidence:
Dispatch validates pincode serviceability, finds nearby partner IDs from current location, filters shifts, checks partner dispatch eligibility, validates required skill by sub-service/category/skill repository, enforces radius and ETA, then revalidates candidate eligibility under `partnerShiftRepository.findByPartnerIdForUpdate(...)` before saving partner requests.

Flow impact:
When scheduled dispatch is actually invoked, partner matching has core serviceability, location, shift/availability, capacity, distance/ETA and skill checks.

### Finding: Scheduled partner request creation is connected

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `createPartnerRequests`, `toPartnerServiceRequest`, `finishDispatch`, `assignScheduledPartnerIfAllowed`

Evidence:
`createPartnerRequests` saves up to five `PartnerServiceRequestEntity` records if no pending request exists for the same order/partner. `toPartnerServiceRequest` copies order ID into `cartId`, customer ID, service ID, quantity, `scheduledTime`, address, city, coordinates, pincode, attempt number, distance, ETA, expiry, and `PENDING` status. `finishDispatch` records `PARTNER_ASSIGNED` for non-instant dispatch and `assignScheduledPartnerIfAllowed` transitions an order from `PARTNER_SEARCH` to `PARTNER_ASSIGNED`.

Flow impact:
Partner request fan-out for scheduled bookings is implemented behind the dispatch call.

### Finding: Partner app request accept/reject APIs are connected for scheduled dispatch requests

PASS:

Partner app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`  
Functions: `acceptDispatchRequest`, `rejectDispatchRequest`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\controller\PartnerRequestController.java`  
Class/methods: `PartnerRequestController#accept`, `PartnerRequestController#reject`

Evidence:
Partner app calls `PATCH /api/v1/partner/requests/{requestId}/accept` with fallback to `/api/partner/requests/{requestId}/accept`. Reject uses the same `/api/v1` then `/api` request route family. Backend `PartnerRequestController` exposes both base mappings `"/api/partner"` and `"/api/v1/partner"` and the matching accept/reject endpoints.

Flow impact:
Once scheduled partner requests exist, the partner app can accept or reject them through backend request endpoints.

### Finding: Scheduled partner acceptance path assigns order

PASS:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`

Evidence:
For non-subscription requests, `accept` locks the partner request using `findByIdAndPartnerIdForUpdate`, locks the order with `orderRepository.findByIdForUpdate`, accepts scheduled/non-instant orders when order status is `CREATED`, `CONFIRMED`, `BOOKING_CREATED`, `SCHEDULED`, `PARTNER_SEARCH`, or `PARTNER_ASSIGNED`, checks partner eligibility under locked shift, marks the request `ACCEPTED`, rejects peer pending requests, and calls `dispatchOrchestrationService.markPartnerAccepted(request.getCartId(), partnerId, etaAtAccept)`.

Flow impact:
Scheduled partner acceptance is connected and uses the same order assignment method as instant once a request exists.

### Finding: Scheduled acceptance can proceed without payment success

FAIL:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`

Evidence:
The payment guard is scoped to instant bookings only: `if (instant && order.getPaymentStatus() != PaymentStatus.SUCCESS)`. For scheduled orders, the method proceeds to partner eligibility and assignment without checking payment status.

Flow impact:
An unpaid scheduled order can be accepted if a partner request has been created. This is a payment/lifecycle risk for scheduled bookings.

### Finding: Scheduled lifecycle after acceptance uses common partner order lifecycle

PARTIAL:

Partner app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`  
Functions: `markOrderArrived`, `startOrder`, `verifyOrderStartPin`, `requestOrderCompletePin`, `verifyOrderCompletePin`, `completeOrder`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class/method: `PartnerOrderService#transition`

Evidence:
Partner app exposes arrive/start/start-verify/complete-pin/complete/complete-verify API calls through common order endpoints. Backend `PartnerOrderService#transition` locks partner orders with `partnerOrderRepository.findByIdForUpdate`, validates ownership through `partnerDispatchEligibilityService.canManageActiveBooking`, and applies lifecycle timestamps during transition.

Flow impact:
The partner lifecycle appears reusable for scheduled orders after assignment. Full enum transition compatibility is deferred to Phase 11 because Phase 2 already recorded a possible common-order vs partner-order status mismatch boundary.

### Finding: Schedule conflict validation service not found

UNKNOWN — CODE EVIDENCE INSUFFICIENT:

Files searched:
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java`

Evidence:
No `SchedulingConflictService` class was found. Searches for conflict/overlap in main Java source did not identify a date/time overlap validator for same partner plus active order. Dispatch eligibility checks shift started/online/service availability/active job capacity, but not overlapping scheduled time windows.

Flow impact:
The code evidence is insufficient to prove scheduled overlap prevention. This must be revisited in Phase 9 with a dedicated schedule-conflict audit across instant, scheduled, subscription, admin, replacement, and redispatch.

### Finding: Scheduled duplicate dispatch protection is partial

PARTIAL:

File: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `dispatchAttemptsForOrder`, `hasPendingRequest`, `createPartnerRequests`

Evidence:
Before creating requests, the service loads existing requests by order ID, excludes partners that already received a request, tracks dispatch attempt number, and skips creating a duplicate pending request for the same order/partner. No unique database constraint evidence was found in Phase 3 for `(orderId, partnerId, status=PENDING)`, and the check is read-before-write.

Flow impact:
Duplicate requests to the same partner are reduced in normal execution, but concurrent manual dispatches can still race unless database constraints or transaction isolation elsewhere prevent it. Classify as partial until Phase 13/14.

### Finding: Scheduled status screen is connected to booking detail/history style lifecycle fetch

PASS:

File: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\booking\ScheduledBookingStatusScreen.tsx`  
Component: `ScheduledBookingStatusScreen`

Evidence:
The scheduled status screen reads `bookingId`, calls `useInstantBookingLifecycle(bookingId)`, runs `useBookingLifecycleNavigation`, displays service, scheduled time, address, amount, refresh, cancel, reschedule, and bookings navigation.

Flow impact:
Customer can see scheduled booking status after creation and can refresh or navigate from the status screen. Despite the hook name, it is reused for scheduled details.

### Phase 3 Interim Verdict

PARTIAL:

Verified connected:
- Customer scheduled slot selection and future-time validation.
- Customer scheduled create request to `/api/v1/bookings/create` with `/api/bookings/create` fallback.
- Backend scheduled booking creation with address ownership/serviceability/geofence checks.
- Order persistence with `bookingType=SCHEDULED`, `scheduledAt`, `PaymentStatus.PENDING`, and dispatch metadata.
- Scheduled order waiting status via `OrderStatus.SCHEDULED`.
- Dispatch window logic exists inside `DispatchOrchestrationService`.
- Manual/admin dispatch can invoke scheduled dispatch.
- Matching checks serviceability, skill, shift, availability, distance, ETA, and active-job capacity.
- Partner request accept path can assign scheduled orders.
- Customer scheduled status screen is connected.

Not fully verified / risks:
- No automatic `ScheduledBookingDispatchScheduler` or equivalent scheduled-order scanner found. Classification: IMPLEMENTED BUT NOT CONNECTED for automatic dispatch.
- Scheduled payment-before-dispatch is not enforced in dispatch or accept paths. Classification: FAIL unless scheduled pay-later is intentional product behavior.
- `SchedulingConflictService` or equivalent date/time overlap validation not found. Classification: UNKNOWN — CODE EVIDENCE INSUFFICIENT until Phase 9.
- Duplicate scheduled dispatch protection is read-before-write and partial. Classification: PARTIAL, to continue in Phase 13/14.
- Partner lifecycle after assignment remains partially verified because full status enum transition compatibility is scheduled for Phase 11.

PHASE 3 COMPLETE

## 5. Subscription Booking Audit

### Phase 4 Scope

Flow traced:
Customer Subscription Screen -> Cart checkout -> `POST /api/subscription/create` -> `SubscriptionBookingService.create()` -> validation -> `SubscriptionMaster` -> `SubscriptionFrequency` -> `SubscriptionVisit` -> payment `Order`.

### Finding: Customer subscription creation screen assembles the expected subscription payload

PASS:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\booking\RecurringBookingScreen.tsx`  
Component: `RecurringBookingScreen`  
Methods/functions: `countVisits`, `onContinue`

Evidence:
The screen tracks `selectedWeekdays`, `startDate`, `endDate`, `durationHours`, `timeSlot`, `repeatServiceMode`, `familySize`, and `effectiveServiceId`. `countVisits` iterates from start date through end date inclusive and increments for selected weekdays. `hasRequiredSelections` requires at least one weekday, at least `MIN_SUBSCRIPTION_VISITS = 3`, a time slot, a service ID, and either `familySize` for cook or `durationHours` for non-cook. `onContinue` sends `serviceId`, `frequencyDays`, `startDate`, `endDate`, computed `visits`, `timeSlot`, `price`, and either cook `familySize` or duration-based fields to `CartCrud`.

Flow impact:
The customer UI captures the main subscription inputs required by the audit: service, service type, frequency days, start/end dates, visit count, duration or family size, and time slot before checkout.

### Finding: Customer cart checkout calls subscription create endpoint and then navigates to payment

PASS:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Component: `CartCrudScreen`  
Code path: subscription branch in checkout handler

Evidence:
When `bookingMode === "SUBSCRIPTION"` or a recurring cart item exists with no normal cart items, the checkout path requires a recurring item, selected address, and booking coordinates. It calls `createSubscriptionBooking` with `serviceId`, `subscriptionServiceType`, `frequencyDays`, `startDate`, `endDate`, `visits`, `durationHours`, `familySize`, `timeSlot`, `addressId`, `latitude`, `longitude`, and `couponCode`. It then reads `bookingId` and `subscriptionId`, locks the cart until payment, and navigates to `Payment` with `bookingMode: "SUBSCRIPTION"` and `postPaymentRoute: "SubscriptionConfirmed"`.

Flow impact:
Customer subscription creation is connected to the payment screen through the backend-created booking/payment order ID.

### Finding: Customer API uses primary and alias subscription creation endpoints

PASS:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\subscriptionBookingApi.ts`  
Function: `createSubscriptionBooking`

Evidence:
`createSubscriptionBooking` builds a body with `bookingMode: "SUBSCRIPTION"`, `serviceType`, `frequencyDays`, `startDate`, `endDate`, `visits`, `durationHours`, `familySize`, `timeSlot`, `addressId`, `latitude`, `longitude`, and trimmed `couponCode`. It attempts `POST /api/subscription/create` first and `POST /api/subscription/bookings` second. It only retries the alias on HTTP `404` or `405`.

Flow impact:
The frontend has a backend-compatible primary endpoint and a creation alias. It avoids retrying creation after validation, network, payment, or server errors, reducing accidental duplicate subscriptions from automatic endpoint fallback.

### Finding: Backend exposes both subscription create route forms behind the same service method

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\controller\SubscriptionController.java`  
Class: `SubscriptionController`  
Endpoints: `POST /api/subscription/create`, `POST /api/subscriptions/create`, `POST /api/subscription/bookings`, `POST /api/subscriptions/bookings`  
Methods: `create`, `createBookingAlias`, `validateSubscriptionEnabled`

Evidence:
The controller has base mappings `"/api/subscription"` and `"/api/subscriptions"`. `@PostMapping("/create")` and `@PostMapping("/bookings")` both validate subscription booking and checkout config flags, then call `subscriptionBookingService.create(customerId, request)`.

Flow impact:
The customer app's `/api/subscription/create` and `/api/subscription/bookings` calls are both backed by the same creation service. The plural `/api/subscriptions/...` forms also exist from the controller base mapping.

### Finding: Subscription request DTO contains the audited creation fields

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\dto\CreateSubscriptionRequest.java`  
Record: `CreateSubscriptionRequest`

Evidence:
The DTO defines `serviceId`, `addressId`, `latitude`, `longitude`, `bookingMode`, `serviceType`, `frequencyDays`, `startDate`, `endDate`, `visits`, `durationHours`, `familySize`, `timeSlot`, `price`, and `couponCode`. Bean validation requires positive `serviceId`, nonblank `addressId`, nonempty `frequencyDays`, nonnull `startDate` and `endDate`, positive `visits` when supplied, positive `durationHours` when supplied, nonblank `timeSlot`, positive `price` when supplied, and max 40-character `couponCode`.

Flow impact:
The request contract contains all Phase 4 input fields. Some fields are advisory because later service logic derives or ignores them, detailed below.

### Finding: Backend verifies active service and subscription support before creation

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Methods: `create`, `supportsSubscription`

Evidence:
`create` loads the service with `serviceRepository.findByIdAndDeletedFalse(request.serviceId())` and rejects missing services. It rejects inactive services and services for which `supportsSubscription(service)` returns false. `supportsSubscription` accepts service type containing `SUBSCRIPTION`, or category names normalized to cleaning, cook, vehicle wash, or fitness/wellness categories.

Flow impact:
Subscription creation is gated by active service existence and a subscription-support rule.

### Finding: Backend verifies address ownership and geofence before persisting the subscription

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Methods: `create`, `resolveAddress`

Evidence:
`create` calls `resolveAddress(customerId, request.addressId(), request.latitude(), request.longitude())` and rejects null with "Address does not belong to logged-in customer". Numeric address IDs are loaded through `addressRepository.findByIdAndUserIdAndIsDeletedFalse`. UUID addresses are resolved from `CustomerAddressRepository` and mirrored into the legacy address table; if the source address is unavailable, the code falls back to an owned legacy address with matching coordinates. After resolution, `geoFenceValidationService.validateBookingAddress(address)` is called and any runtime validation error is converted to `SubscriptionValidationException`.

Flow impact:
Subscription creation checks customer ownership for numeric IDs and mirrored UUID customer-address IDs, then enforces serviceability/geofence validation before any payment order or subscription rows are saved.

### Finding: Backend validates date ordering but does not enforce future start date

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `validateBasicRequest`

Evidence:
`validateBasicRequest` rejects `startDate` after `endDate`, blank `timeSlot`, and empty normalized frequency days. It does not check `startDate` or `endDate` against `LocalDate.now()`.

Flow impact:
The frontend initializes start date to today and blocks end dates before start date, but backend source evidence does not prevent API clients from creating subscriptions with a past date range if the range is otherwise valid. This is a backend validation gap.

### Finding: Backend ignores request.visits and computes visits from date range plus frequency days

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `create`

Evidence:
`create` normalizes `request.frequencyDays()`, generates dates using `SubscriptionScheduleSupport.generateVisitDates(request.startDate(), request.endDate(), frequencyDays)`, assigns `int visitCount = generatedDates.size()`, and stores `master.setVisits(visitCount)`. It never compares `request.visits()` to `visitCount`.

Flow impact:
The persisted subscription visit count is mathematically derived by backend code, which is good for server authority. However, a client-supplied `visits` mismatch is not rejected, so the API contract's `visits` field is not an enforced invariant. If the UI quote was based on a different count, the backend will price using the generated count instead.

### Finding: Visit generation is deterministic and inclusive of start/end dates

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionScheduleSupport.java`  
Class: `SubscriptionScheduleSupport`  
Methods: `normalizeDays`, `parseDay`, `generateVisitDates`

Evidence:
`normalizeDays` parses each input day through `parseDay`, stores display names in insertion order using `LinkedHashSet`, and deduplicates repeated days. `generateVisitDates` converts normalized day names back to `DayOfWeek`, starts at `startDate`, loops while `!cursor.isAfter(endDate)`, and adds the cursor date only when its weekday is selected.

Flow impact:
For a plan such as MONDAY + WEDNESDAY, the backend will create exactly one visit for each Monday or Wednesday between start and end dates inclusive. If that range contains exactly 8 matching weekdays, exactly 8 `SubscriptionVisit` rows are generated.

### Finding: Backend creates payment Order, SubscriptionMaster, frequency rows, and visit rows in one transaction

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `create`

Evidence:
`create` is annotated `@Transactional`. It creates an `Order` with customer ID, `OrderStatus.CONFIRMED`, `PaymentStatus.PENDING`, `bookingType=SUBSCRIPTION`, `scheduledAt=startDate.atStartOfDay()`, formatted address, total amount from the price selection, coupon application, and one `OrderItem`. It saves the order through `orderRepository.save`. It then creates `SubscriptionMaster` with customer/service/address/booking IDs, booking mode, service type, start/end dates, computed visits, duration/family size, per-visit price, time slot, total price, and `SubscriptionStatus.PENDING`. It saves frequency rows and generated visit rows with `VisitStatus.SCHEDULED`.

Flow impact:
The creation phase persists a pending payment booking/order and pending subscription metadata atomically. The subscription is not active at creation time.

### Finding: Pricing is server-derived from configured per-visit pricing, not trusted from request.price

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Methods: `create`, `resolvePrice`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionPricingService.java`  
Class: `SubscriptionPricingService`  
Methods: `resolvePerVisitPrice`, `resolveCookPricePerVisit`

Evidence:
`resolvePrice` uses the generated visit count. For `COOK`, it requires `familySize`, calls `resolveCookPricePerVisit`, and multiplies per-visit price by visits. For non-cook service types, it requires `durationHours`, calls `resolvePerVisitPrice`, and multiplies per-visit price by visits. `SubscriptionPricingService` resolves active configured prices by service ID with fallback to global rows. `request.price()` is present in the DTO but is not used in `SubscriptionBookingService.create`.

Flow impact:
Subscription amount is calculated on the backend from configured pricing, reducing client tampering risk. The client `price` field is redundant for creation.

### Finding: Service type is mostly client-controlled when supplied

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `resolveServiceType`

Evidence:
If `request.serviceType()` is nonblank, the backend trims and uppercases it and uses that value. Only when service type is blank does it infer `COOK` from service category/name or default to `CLEANING`. `resolvePrice` treats only exact `COOK` as cook pricing; all other service types use duration-hour pricing.

Flow impact:
The subscription service type is accepted from the client even if it does not match the catalog service category. This may be acceptable for the current UI, but backend evidence does not show a strict service-type-to-service-category validation.

### Finding: Subscription entities persist the required master/frequency/visit data, but duplicate visit-date uniqueness is not enforced

PARTIAL:

Backend files:  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\entity\SubscriptionMaster.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\entity\SubscriptionFrequency.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\entity\SubscriptionVisit.java`

Evidence:
`SubscriptionMaster` maps `subscription_master` with customer, service, address, booking, booking mode, service type, start/end dates, visits, duration hours, family size, per-visit price, time slot, total price, and status. `SubscriptionFrequency` maps `subscription_id` and `day_name`. `SubscriptionVisit` maps `subscription_id`, unique `booking_id`, `visit_date`, `time_slot`, status, and rescheduled date. The visit table has an index on `(subscription_id, visit_date)` but no unique constraint on that pair in the entity.

Flow impact:
Creation persists the expected subscription records. Within the creation path, duplicate days are normalized before visit generation, so normal creation should not create duplicate dates. At the database level, duplicate visit dates for the same subscription are not blocked by a unique constraint.

### Finding: Duplicate subscription creation protection was not found in Phase 4

IDEMPOTENCY RISK:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `create`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\repository\SubscriptionMasterRepository.java`  
Interface: `SubscriptionMasterRepository`

Evidence:
`create` directly creates a new payment `Order`, `SubscriptionMaster`, `SubscriptionFrequency` rows, and `SubscriptionVisit` rows. Phase 4 found no idempotency key, duplicate request token, matching existing-pending-subscription lookup, or unique constraint in `SubscriptionMaster` for customer/service/address/date range/time slot. `SubscriptionMasterRepository` contains lookup methods by customer, status, ID, and booking ID only.

Flow impact:
If the customer submits the same subscription creation request twice, backend code evidence indicates two pending subscription/payment orders can be created. The frontend reduces automatic endpoint fallback duplication, but backend creation itself is not idempotent.

### Finding: Payment provider order creation is not performed inside subscription creation service

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `create`  
Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\cart\CartCrudScreen.tsx`  
Code path: subscription payment navigation

Evidence:
`create` creates an internal `Order` with `PaymentStatus.PENDING` and returns `bookingId`. The customer app then navigates to `Payment` with that `bookingId`, `amount`, `bookingMode: "SUBSCRIPTION"`, and `postPaymentRoute: "SubscriptionConfirmed"`. Phase 4 did not find a Razorpay/payment-provider order being created inside `SubscriptionBookingService.create`.

Flow impact:
Subscription creation produces the internal pending payment order, while external payment initialization appears deferred to the common payment screen/API path. Payment callback and activation are deferred to Phase 5.

### Phase 4 Interim Verdict

PARTIAL:

Verified connected:
- Customer recurring booking screen builds subscription inputs.
- Cart checkout calls subscription creation and navigates to payment with the returned booking ID.
- Backend exposes `/api/subscription/create` and `/api/subscription/bookings`, with plural base aliases also present.
- Backend validates active service, subscription-support rule, customer address ownership/mirroring, and geofence.
- Backend computes visits from frequency days and date range, stores `SubscriptionMaster`, `SubscriptionFrequency`, and `SubscriptionVisit` rows, and creates an internal pending payment `Order`.
- Backend derives price from configured per-visit pricing instead of trusting request price.

Not fully verified / risks:
- Backend does not enforce future start/end dates in subscription creation.
- Backend does not compare `request.visits` to generated visit count; it silently uses generated count.
- Client-supplied `serviceType` is accepted when nonblank without strict catalog-category validation.
- No backend idempotency or duplicate subscription creation protection was found for repeated create requests.
- External payment-provider order initialization is not in `SubscriptionBookingService.create`; payment activation must be audited in Phase 5.

PHASE 4 COMPLETE

### Phase 5 Scope

Flow traced:
Subscription -> payment order -> checkout verification / Razorpay webhook -> payment captured event -> subscription activation -> first-visit dispatch attempt.

Critical rule checked:
UNPAID SUBSCRIPTION MUST NOT BECOME AN ACTIVE SERVICE DISPATCH unless code explicitly defines otherwise.

### Finding: Customer payment screen creates a Razorpay order and verifies checkout payment for subscriptions

PASS:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\screens\payment\PaymentScreen.tsx`  
Component: `PaymentScreen`  
Functions: `payMutation`, `navigateAfterPayment`  
Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\paymentApi.ts`  
Functions: `createPaymentOrder`, `verifyPayment`

Evidence:
The subscription checkout path from Phase 4 navigates to `Payment` with `bookingMode: "SUBSCRIPTION"` and `postPaymentRoute: "SubscriptionConfirmed"`. `PaymentScreen` calls `createPaymentOrder({ bookingId }, idempotencyKey)`, opens Razorpay checkout, requires `orderId`, `paymentId`, and `signature`, then calls `verifyPayment` with the Razorpay order/payment/signature. On success, `navigateAfterPayment` routes to `SubscriptionConfirmed` with the subscription ID.

Flow impact:
Subscription payment goes through the common payment screen and backend `/api/payments/create-order` plus `/api/payments/verify` APIs.

### Finding: Payment order creation is idempotent per Idempotency-Key and sets booking payment to PENDING

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\controller\PaymentController.java`  
Class: `PaymentController`  
Endpoint: `POST /api/payments/create-order`, `POST /api/v1/payments/create-order`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Method: `createOrder`

Evidence:
`PaymentController#createOrder` requires the `Idempotency-Key` header and calls `paymentService.createOrder(customerId, request.bookingId(), idempotencyKey)`. `PaymentService#createOrder` rejects a blank key, returns an existing transaction found by `transactionRepository.findByIdempotencyKey`, locks the owned booking through `findOwnedBookingForUpdate`, creates a Razorpay order, saves a `PaymentTransaction`, sets `booking.setPaymentStatus(PaymentStatus.PENDING)`, and saves the booking.

Flow impact:
Payment order creation is idempotent for a repeated key, but the mobile app generates a new key per payment attempt, so user retries intentionally create new payment transactions.

### Finding: Checkout payment verification marks booking paid only after signature, Razorpay fetch, order ID, and amount validation

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Methods: `verify`, `validateFetchedPayment`, `markPaid`

Evidence:
`verify` loads the payment transaction by Razorpay order ID, checks customer ownership, short-circuits already `PAID` transactions, verifies the Razorpay checkout signature, fetches the payment from Razorpay, calls `validateFetchedPayment`, saves Razorpay payment/signature fields, and calls `markPaid` only when `payment.isCaptured()` is true. `validateFetchedPayment` rejects mismatched Razorpay order IDs and mismatched amounts. `markPaid` sets transaction status `PAID`, locks the booking, sets booking `PaymentStatus.SUCCESS`, and publishes `PaymentCapturedEvent` only when the transaction was not already paid.

Flow impact:
Subscription activation through the in-app verify path is gated by actual captured payment evidence and amount/order consistency.

### Finding: Razorpay webhook capture path also marks booking paid and publishes the same captured event

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\webhook\RazorpayWebhookController.java`  
Class: `RazorpayWebhookController`  
Endpoints: `POST /api/payments/webhook/razorpay`, `POST /api/payments/webhook`, `POST /api/v1/webhooks/razorpay`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Methods: `processWebhook`, `handleCaptured`, `markPaid`

Evidence:
`RazorpayWebhookController#webhook` passes `X-Razorpay-Event-Id`, `X-Razorpay-Signature`, and raw body to `paymentService.processWebhook`. `processWebhook` verifies the webhook signature, extracts event/payment IDs, stores a `PaymentWebhookEvent`, and processes `payment.captured` via `handleCaptured`. `handleCaptured` locks the transaction by Razorpay order ID, ignores it if already `PAID`, otherwise calls `markPaid`, which sets booking `PaymentStatus.SUCCESS` and publishes `PaymentCapturedEvent`.

Flow impact:
Subscription activation is reachable from both frontend payment verification and backend Razorpay webhook capture.

### Finding: Duplicate captured payments are guarded before republishing activation event

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Methods: `verify`, `processWebhook`, `handleCaptured`, `markPaid`  
Backend files:  
`C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\entity\PaymentTransaction.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\entity\PaymentWebhookEvent.java`

Evidence:
`verify` returns immediately when a transaction is already `PaymentTransactionStatus.PAID`. `handleCaptured` checks `transaction.getPaymentStatus() == PaymentTransactionStatus.PAID` and returns without calling `markPaid`. `markPaid` computes `newlyPaid` before changing status and publishes `PaymentCapturedEvent` only when `newlyPaid` is true. `PaymentTransaction` has unique columns for `idempotency_key`, `razorpay_order_id`, and `razorpay_payment_id`. `PaymentWebhookEvent` has unique `event_id` and `idempotency_key`; webhook processing locks by idempotency key and returns duplicate for existing `SUCCESS` or `PROCESSING` events.

Flow impact:
Duplicate callbacks for the same successful payment should not repeatedly activate the subscription or repeatedly dispatch the first visit through `PaymentCapturedEvent`.

### Finding: Payment failed events do not activate subscriptions

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Methods: `verify`, `handleFailed`, `markFailed`

Evidence:
If checkout signature verification fails, `verify` calls `markFailed` and returns an unverified response. `handleFailed` processes Razorpay `payment.failed` only when the transaction is not already `PAID`. `markFailed` sets transaction status `FAILED`, sets booking `PaymentStatus.FAILED`, publishes `PaymentFailedEvent`, and increments failure metrics. There is no subscription activation call in the failed-payment path.

Flow impact:
A normal payment failure leaves the subscription unactivated. Activation requires `PaymentCapturedEvent`, which is only published by `markPaid`.

### Finding: Subscription activation listener is connected to captured payment events

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionPaymentListener.java`  
Class: `SubscriptionPaymentListener`  
Method: `onPaymentCaptured`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionBookingService.java`  
Class: `SubscriptionBookingService`  
Method: `activateByBookingId`

Evidence:
`SubscriptionPaymentListener#onPaymentCaptured` is annotated `@EventListener` and calls `subscriptionBookingService.activateByBookingId(event.bookingId())`. `activateByBookingId` loads `SubscriptionMaster` by `bookingId` and changes status from `PENDING` to `ACTIVE`; other statuses are left unchanged.

Flow impact:
The subscription master moves from pending to active after payment capture. The activation method is idempotent for duplicate events because it only changes `PENDING` subscriptions.

### Finding: First-visit auto-dispatch is triggered immediately after payment capture

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionPaymentListener.java`  
Class: `SubscriptionPaymentListener`  
Method: `onPaymentCaptured`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `dispatchForBooking`

Evidence:
After activation, `onPaymentCaptured` calls `subscriptionAutoDispatchService.dispatchForBooking(event.bookingId())`. `dispatchForBooking` finds the subscription by booking ID, returns `0` unless status is `ACTIVE`, loads the first `SCHEDULED` or `RESCHEDULED` visit, and calls `autoAssignVisit`. `autoAssignVisit` skips visits with an existing visit assignment or active request, then routes to request creation.

Flow impact:
Subscription payment success can immediately create partner request(s) for the first scheduled/rescheduled visit.

### Finding: Auto-dispatch itself does not re-check payment status

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `autoAssignDueVisits`, `dispatchForBooking`, `autoAssignVisit`, `createDispatchRequests`

Evidence:
`autoAssignDueVisits` loads subscriptions by `SubscriptionStatus.ACTIVE`. `dispatchForBooking` checks only that the subscription exists and has status `ACTIVE`. `autoAssignVisit` checks existing assignments/active requests, but the Phase 5 source evidence did not show a lookup of the parent payment order or a `PaymentStatus.SUCCESS` check before `createDispatchRequests` saves `PartnerServiceRequestEntity` rows.

Flow impact:
In the normal payment flow, activation happens only after successful payment, so this is connected. However, if a subscription becomes `ACTIVE` through an incorrect direct status mutation, admin path, data repair, or bug, auto-dispatch can create partner requests without revalidating payment success. This is a defense-in-depth gap; later admin/state-machine phases must check whether such an activation bypass exists.

### Finding: Unpaid subscription visit cannot create the actual service order on partner acceptance

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`

Evidence:
`accept` locks the visit using `visits.findByIdForUpdate`, returns existing visit order only for the same assigned partner, requires subscription status `ACTIVE`, requires visit status `SCHEDULED` or `RESCHEDULED`, loads the parent payment booking with `orders.findById(subscription.getBookingId())`, and throws `BadRequestException("Subscription payment has not been verified")` unless `payment.getPaymentStatus() == PaymentStatus.SUCCESS`. Only after that check does it create the partner order, set order payment status `SUCCESS`, set `visit.bookingId`, and mark the visit `ACCEPTED`.

Flow impact:
Even if an unpaid active subscription somehow creates a partner request, the actual normal service order for the visit is blocked at partner acceptance unless the parent subscription payment order is successful.

### Finding: Failed checkout reporting endpoint appears missing from backend payment controller

FRONTEND-ONLY:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\paymentApi.ts`  
Function: `reportPaymentFailure`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\controller\PaymentController.java`  
Class: `PaymentController`

Evidence:
The customer app attempts `POST /api/payments/failure`, `/api/payments/payment-failed`, and `/api/payment/failure` after a Razorpay checkout failure. Phase 5 did not find matching endpoints in `PaymentController`; the backend failure state is updated through signature verification failure or Razorpay `payment.failed` webhook.

Flow impact:
Checkout-cancel/failure reporting from mobile is best-effort and may fail silently, so a failed/cancelled checkout can leave the internal payment transaction in `CREATED` or booking in `PENDING` until webhook/follow-up payment status handling occurs.

### Finding: Payment webhook retry exists for transient processing failures

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\service\PaymentService.java`  
Class: `PaymentService`  
Methods: `processWebhook`, `retryDueWebhookEvents`, `markWebhookFailed`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\payment-service\src\main\java\com\lexho\payment\webhook\PaymentWebhookRetryScheduler.java`  
Class: `PaymentWebhookRetryScheduler`  
Method: `retryFailedWebhookEvents`

Evidence:
`processStoredWebhookEvent` marks successful processing as `SUCCESS`; on runtime exception, `markWebhookFailed` increments retry count and schedules `nextRetryAt` using retry delays of 1, 5, 15, 30, and 60 minutes before dead-lettering. `PaymentWebhookRetryScheduler` runs with `@Scheduled(fixedDelayString = "${app.payments.webhook.retry-scheduler-delay-ms:60000}")` and calls `paymentService.retryDueWebhookEvents()`.

Flow impact:
If subscription activation fails because webhook processing fails before successful captured-event handling, the payment webhook has a retry path. If listener-side activation/dispatch throws after `markPaid`, transactional event behavior needs deeper review in Phase 12/14.

### Phase 5 Interim Verdict

PARTIAL:

Verified connected:
- Subscription payment uses common payment order creation and checkout verification.
- Payment order creation is idempotent per `Idempotency-Key`.
- Successful checkout verification and Razorpay `payment.captured` webhook both mark the booking `PaymentStatus.SUCCESS`.
- `PaymentCapturedEvent` activates pending subscription masters.
- Duplicate payment capture handling avoids repeated captured events for already-paid transactions.
- Payment failure does not activate the subscription.
- First-visit dispatch is triggered after activation.
- Partner acceptance for subscription visits blocks actual visit order creation unless the parent subscription payment booking has `PaymentStatus.SUCCESS`.

Not fully verified / risks:
- `SubscriptionAutoDispatchService` creates partner requests based on `SubscriptionStatus.ACTIVE` without independently checking parent payment success.
- Mobile checkout failure reporting endpoints appear frontend-only; backend relies on verification failure or Razorpay failed-payment webhook.
- Listener transaction phase differs from the instant booking payment listener: subscription listener uses plain `@EventListener`, while instant dispatch listener uses `@TransactionalEventListener(AFTER_COMMIT)`. This may matter if activation/dispatch side effects throw inside the payment transaction; defer deeper transaction-boundary assessment to Phase 12/14.

PHASE 5 COMPLETE

## 6. Partner Management Audit

## 7. Payment Audit

## 8. Dispatch Audit

## Phase 6 - SUBSCRIPTION AUTO-DISPATCH

### Finding: Subscription auto-dispatch scheduler is connected and scans once per fixed delay

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\scheduler\SubscriptionAutoDispatchScheduler.java`  
Class: `SubscriptionAutoDispatchScheduler`  
Method: `autoAssignDueVisits`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `autoAssignDueVisits`

Evidence:
`SubscriptionAutoDispatchScheduler#autoAssignDueVisits` is annotated with `@Scheduled(fixedDelayString = "${app.subscription.auto-dispatch.scan-ms:60000}")` and delegates directly to `subscriptionAutoDispatchService.autoAssignDueVisits()`. The service method loads active subscriptions via `subscriptionMasterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE)`.

Flow impact:
Subscription auto-dispatch is not a frontend-triggered operation. It is backend scheduled polling with a default fixed delay of 60,000 ms after each run completes.

### Finding: `scan-ms` behavior is defaulted in scheduler annotation, not declared in application.properties

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\scheduler\SubscriptionAutoDispatchScheduler.java`  
Class: `SubscriptionAutoDispatchScheduler`  
Method: `autoAssignDueVisits`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\resources\application.properties`

Evidence:
The scheduler annotation uses `${app.subscription.auto-dispatch.scan-ms:60000}`. `application.properties` contains dispatch tuning values for `partner.initial-search-radius-km`, `partner.max-search-radius-km`, `booking.max-eta-minutes`, and `booking.pre-dispatch-minutes`, but Phase 6 source search did not find an explicit `app.subscription.auto-dispatch.scan-ms` property in that file.

Flow impact:
If no external environment/config value is supplied, the scheduler runs with a 60-second fixed delay. Because Spring fixed delay waits until the previous execution finishes before starting the next execution on the same scheduler thread, it reduces same-thread overlap, but it does not by itself protect against multiple application instances running the scheduler concurrently.

### Finding: Due visit detection uses ACTIVE subscription status plus visit status SCHEDULED/RESCHEDULED

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `autoAssignDueVisits`

Evidence:
`autoAssignDueVisits` loads `SubscriptionStatus.ACTIVE` subscriptions, maps them by ID, and loads visits with `findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(subscriptionIds, List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED))`. It skips visits that already have a visit assignment, skips visits with active requests, then requires `isDueForDispatch(visit)` before attempting dispatch.

Flow impact:
Cancelled, accepted, in-progress, and completed visits are not candidates in the scheduled scan. ACTIVE subscription status is the subscription-level gate.

### Finding: Dispatch window is calculated from parsed visit time slot minus pre-dispatch minutes

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `isDueForDispatch`, `scheduledAt`, `parseStartTime`, `getPreDispatchMinutes`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\resources\application.properties`

Evidence:
`isDueForDispatch` builds `scheduledAt` from `visit.getVisitDate()` and the parsed start of `visit.getTimeSlot()`, then returns true when current time is not before `scheduledAt.minusMinutes(getPreDispatchMinutes())`. `parseStartTime` supports `H:mm`, `HH:mm`, `h:mm a`, and `hh:mm a` and splits ranges on `-`. `getPreDispatchMinutes` clamps the property to 5-240 minutes. `application.properties` sets `booking.pre-dispatch-minutes=${BOOKING_PRE_DISPATCH_MINUTES:${APP_DISPATCH_SCHEDULED_PRE_DISPATCH_MINUTES:30}}`.

Flow impact:
Default behavior is to begin subscription partner request creation 30 minutes before the visit start time. An unparsable time slot makes `scheduledAt` null and the visit is silently skipped by auto-dispatch.

### Finding: First-visit dispatch after payment does not use the due-window check

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `dispatchForBooking`

Evidence:
`dispatchForBooking` loads the subscription by booking ID, requires status `ACTIVE`, retrieves the first `SCHEDULED` or `RESCHEDULED` visit by visit date/ID, and calls `autoAssignVisit(subscription.getId(), visit.getId())`. `autoAssignVisit` checks existence, visit assignment, and active request, but does not call `isDueForDispatch`.

Flow impact:
The scheduled scanner respects the pre-dispatch window, but payment-captured first-visit dispatch can create a partner request for the first visit immediately after activation, even if the first visit is outside the normal dispatch window. This may be intentional for immediate first visits, but Phase 6 code does not enforce the same window.

### Finding: Auto-dispatch creates one request per attempt and avoids already-requested partners

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `createDispatchRequests`, `hasActiveRequest`

Evidence:
`createDispatchRequests` loads existing requests for the visit with `findBySubscriptionVisitId(visit.getId())`, builds `requestedPartnerIds`, computes `nextAttempt` from the maximum prior dispatch attempt plus one, and excludes previously requested partner IDs when falling back. It saves one `PartnerServiceRequestEntity` for the preferred partner if eligible; otherwise it saves one fallback request.

Flow impact:
The implementation dispatches sequentially, not as a broadcast. Retry attempts avoid partners already requested for that visit.

### Finding: Preferred partner path is implemented before nearest fallback

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `createDispatchRequests`, `preferredPartnerId`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\repository\SubscriptionPartnerAssignmentRepository.java`  
Repository: `SubscriptionPartnerAssignmentRepository`

Evidence:
`createDispatchRequests` loads `subscriptionPartnerAssignmentRepository.findBySubscriptionId(subscription.getId())`. `preferredPartnerId` returns `replacementPartnerId` when present, otherwise `partnerId`. If the preferred partner has not already been requested and passes `isEligiblePartner`, the service saves a request with reason `AUTO_PRIMARY_PARTNER` and returns.

Flow impact:
Existing subscription partner assignment is authoritative for first dispatch preference, and replacement partner IDs supersede original partner IDs.

### Finding: Fallback matching uses current location radius, partner profile, shift, skill, distance, ETA, rating, and active jobs

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `findNearestEligiblePartner`, `nearbyCandidates`, `isEligiblePartner`  
Backend files:  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\repository\CurrentPartnerLocationRepository.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\repository\PartnerServiceSkillRepository.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\PartnerDispatchEligibilityService.java`

Evidence:
Fallback calls `currentPartnerLocationRepository.findNearbyPartnerIds(lat, lng, radiusKm, 200)`. Each candidate must pass `partnerDispatchEligibilityService.canReceiveNewBookings(profile, shift)`, `partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, subscription.getServiceId())`, current location presence, max radius, and max ETA. Eligible candidates are sorted by distance ascending, rating descending, then active jobs ascending.

Flow impact:
Subscription dispatch has concrete skill, online/profile, shift, distance, ETA, and active-job filtering before creating partner requests.

### Finding: Availability check does not include partner leave in auto-dispatch

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `isEligiblePartner`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `getIncomingRequests`

Evidence:
`SubscriptionAutoDispatchService#isEligiblePartner` checks operational profile, shift, skill, current location, radius, and ETA. It does not inject or query `PartnerLeaveRepository`. `PartnerRequestService#getIncomingRequests` checks `partnerLeaveRepository.existsByPartnerIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(...)` and returns an empty page when the partner is on leave.

Flow impact:
Auto-dispatch can create a pending request for a partner who is on leave, but the partner incoming-request listing hides requests while leave is active. This can cause request expiry/retry delay rather than precluding the partner at matching time.

### Finding: Active request guard blocks duplicate dispatch while a PENDING or ACCEPTED request exists

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `autoAssignDueVisits`, `autoAssignVisit`, `retryDispatch`, `hasActiveRequest`

Evidence:
Before creating requests, `autoAssignDueVisits`, `autoAssignVisit`, and `retryDispatch` all check whether a visit assignment exists or an active request exists. `hasActiveRequest` returns true when count of `PENDING` or `ACCEPTED` requests for the visit is greater than zero.

Flow impact:
Normal repeated scheduler runs do not create another partner request for the same visit while a current pending/accepted request remains.

### Finding: Request expiry/rejection retries subscription dispatch

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `reject`, `expirePendingRequests`, `expireIfNeeded`, `maybeContinueRequestDispatch`, `maybeContinueSubscriptionDispatch`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\scheduler\PartnerRequestExpiryScheduler.java`  
Class: `PartnerRequestExpiryScheduler`  
Method: `expirePendingRequests`

Evidence:
Partner rejection of a subscription request calls `maybeContinueRequestDispatch`. Expiry through `expirePendingRequests` also calls `maybeContinueRequestDispatch` for each expired request. `maybeContinueSubscriptionDispatch` counts pending and accepted requests for the visit and calls `subscriptionAutoDispatchService.retryDispatch(subscriptionId, visitId)` only when both counts are zero. The expiry scheduler runs with `@Scheduled(fixedDelayString = "${app.partner.requests.expiry-scan-ms:30000}")`.

Flow impact:
Subscription dispatch can continue to a fallback partner after rejection or expiry. The default pending request response window is two minutes in `SubscriptionAutoDispatchService`, while the expiry scanner default is 30 seconds.

### Finding: Actual subscription visit order creation rechecks payment success and locks the visit

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\repository\SubscriptionVisitRepository.java`  
Repository: `SubscriptionVisitRepository`

Evidence:
`SubscriptionVisitOrderService#accept` loads the visit through `visits.findByIdForUpdate(request.getSubscriptionVisitId())`, requires active subscription and `SCHEDULED`/`RESCHEDULED` visit state, loads the parent payment booking, and throws unless `payment.getPaymentStatus() == PaymentStatus.SUCCESS`. Only then does it create the `PartnerOrder`, set order payment status `SUCCESS`, set `visit.bookingId`, and set `visit.status = VisitStatus.ACCEPTED`.

Flow impact:
Even if auto-dispatch creates a partner request for an incorrectly active unpaid subscription, partner acceptance cannot create the visit order without successful parent payment.

### Finding: Auto-dispatch itself still does not re-check payment success

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `autoAssignDueVisits`, `dispatchForBooking`, `autoAssignVisit`, `createDispatchRequests`

Evidence:
The auto-dispatch path gates on `SubscriptionStatus.ACTIVE`, visit state, address coordinates, request/assignment checks, and partner eligibility. Phase 6 source evidence did not show an `OrderRepository` lookup or parent booking `PaymentStatus.SUCCESS` check inside `SubscriptionAutoDispatchService`.

Flow impact:
The final order creation is protected in `SubscriptionVisitOrderService`, but partner request creation can still occur based only on active subscription status. This remains a defense-in-depth gap if some later state-machine/admin path can mark a subscription active without successful payment.

### Finding: Existing order and assignment checks are not protected by visit locks in auto-dispatch

CONCURRENCY RISK:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `autoAssignDueVisits`, `autoAssignVisit`, `retryDispatch`, `markPartnerAccepted`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\repository\SubscriptionVisitRepository.java`  
Repository: `SubscriptionVisitRepository`

Evidence:
`SubscriptionVisitRepository` has `findByIdForUpdate`, but `SubscriptionAutoDispatchService` does not use it. The dispatch methods check `visitPartnerAssignmentRepository.findByVisitId(visitId)` and `hasActiveRequest(visitId)` before saving, but these are read-check-write operations without a visit row lock. `markPartnerAccepted` also checks `findByVisitId` before saving a unique `visit_id` assignment.

Flow impact:
Two scheduler executions or two app instances can both observe no assignment and no active request, then both create requests for the same visit. The unique `subscription_visit_partner_assignment.visit_id` constraint protects the final assignment row, but not duplicate pending request creation.

### Finding: Database uniqueness exists for one visit assignment, but not one subscription request per visit

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\resources\db\migration\application-module\V65__subscription_partner_assignment.sql`  
Migration: `V65__subscription_partner_assignment`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\resources\db\migration\application-module\V103__partner_service_requests_subscription_context.sql`  
Migration: `V103__partner_service_requests_subscription_context`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\resources\db\migration\application-module\V154__instant_booking_payment_dispatch_safety.sql`  
Migration: `V154__instant_booking_payment_dispatch_safety`

Evidence:
`V65` creates `UNIQUE KEY uk_subscription_visit_partner_assignment_visit (visit_id)`. `V103` adds `subscription_id`, `subscription_visit_id`, and `CREATE INDEX idx_psr_subscription_visit_status ON partner_service_requests(subscription_visit_id, status)` but no unique request constraint. `V154` creates a generated `accepted_cart_id` only when `status = 'ACCEPTED' AND subscription_visit_id IS NULL`, then creates `uq_partner_request_one_accepted_order` on that generated column, explicitly excluding subscription requests.

Flow impact:
The database prevents two final visit assignment rows for one visit, but it does not prevent multiple pending or accepted `partner_service_requests` rows for the same subscription visit under concurrent dispatch or acceptance races.

### Finding: Partner acceptance locks the request row and the visit row, then rejects peer subscription requests

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `accept`, `rejectPeerSubscriptionRequests`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`

Evidence:
`PartnerRequestService#accept` loads the request via `findByIdAndPartnerIdForUpdate`. For subscription requests, it locks the partner profile and shift, then calls `subscriptionVisitOrders.accept(request, partnerId)`. `SubscriptionVisitOrderService#accept` locks the visit by `findByIdForUpdate`, returns the existing order ID for the same assigned partner, and rejects a different partner when `visit.bookingId` is already set. After the request is accepted, `PartnerRequestService#rejectPeerSubscriptionRequests` rejects other pending requests for the same subscription visit.

Flow impact:
Acceptance has stronger protection than dispatch creation. The visit row lock serializes creation of the actual visit order.

### Finding: Schedule conflict is not independently checked during subscription auto-dispatch

UNKNOWN — CODE EVIDENCE INSUFFICIENT:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `isEligiblePartner`, `createDispatchRequests`

Evidence:
Phase 6 search did not find a `SchedulingConflictService` class or a conflict/overlap check in `SubscriptionAutoDispatchService`. Candidate filtering considers shift active state and `activeJobsCount`, but no code evidence in this service checks whether the same partner already has another accepted/scheduled order overlapping the visit date/time.

Flow impact:
Auto-dispatch may rely only on `activeJobsCount` and partner shift/profile state for availability. A deeper Phase 9 schedule-conflict audit must verify whether any other layer prevents overlapping partner assignments for subscription visits.

### Finding: Failure isolation across candidate visits is weak because the full scan is one transaction without per-visit catch

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `autoAssignDueVisits`

Evidence:
`autoAssignDueVisits` is annotated `@Transactional` and loops through all candidate visits. The method does not wrap per-visit dispatch in a try/catch. Runtime exceptions thrown while processing one visit can abort the transaction for the whole scan.

Flow impact:
A bad row or unexpected persistence/runtime error for one candidate can prevent later due visits from being assigned in the same scheduler run. The next scan may retry, but same-run failure isolation is not implemented.

### Finding: Subscription auto-dispatch has focused unit tests for primary, fallback, and future/assigned skip behavior

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchServiceTest.java`  
Class: `SubscriptionAutoDispatchServiceTest`

Evidence:
Tests cover primary assigned partner request creation, fallback to nearest eligible partner, and skipping future/already assigned visits. The tests verify saved request fields such as partner ID, `AUTO_PRIMARY_PARTNER`, `AUTO_FALLBACK_DISPATCH`, subscription ID, and subscription visit ID.

Flow impact:
The happy path and two core filters are covered, but Phase 6 did not find tests for concurrent scheduler execution, duplicate request creation, payment recheck inside auto-dispatch, malformed time slots, partner leave exclusion, or dispatch window bypass through `dispatchForBooking`.

### Phase 6 Interim Verdict

PARTIAL:

Verified connected:
- `SubscriptionAutoDispatchScheduler` is connected to `SubscriptionAutoDispatchService#autoAssignDueVisits`.
- Default scheduler fixed delay is 60 seconds through `${app.subscription.auto-dispatch.scan-ms:60000}`.
- Due visit detection uses ACTIVE subscriptions and SCHEDULED/RESCHEDULED visits.
- Scheduled scanner respects the pre-dispatch window based on visit date/time slot and `booking.pre-dispatch-minutes`.
- Preferred assigned/replacement partner is tried first.
- Fallback matching checks current location radius, partner profile, shift/online status, active job capacity, skill, distance, ETA, rating, and active jobs.
- Active pending/accepted requests block normal duplicate dispatch.
- Rejection and expiry paths can trigger retry dispatch.
- Partner acceptance locks the request and visit; actual visit order creation rechecks parent payment success.

Risks / gaps:
- `dispatchForBooking` bypasses `isDueForDispatch` and can dispatch the first visit immediately after payment activation.
- `SubscriptionAutoDispatchService` creates partner requests based on ACTIVE subscription status without independently checking parent payment success.
- Auto-dispatch does not use `SubscriptionVisitRepository#findByIdForUpdate`, so concurrent scheduler/app-instance executions can duplicate pending requests.
- Database uniqueness protects one `subscription_visit_partner_assignment` per visit, but not one pending/accepted `partner_service_requests` row per subscription visit.
- Partner leave is not part of auto-dispatch eligibility; requests can be created and then hidden from the partner while leave is active.
- Schedule conflict/overlap checking was not found in the auto-dispatch path.
- The scheduled scan is one transaction without per-visit failure isolation.
- Unit tests do not cover concurrency, payment recheck at dispatch, partner leave, malformed slot parsing, or `dispatchForBooking` window bypass.

PHASE 6 COMPLETE

## Phase 7 - SUBSCRIPTION PARTNER FLOW

### Finding: Partner app request endpoints are connected to subscription request acceptance

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\controller\PartnerRequestController.java`  
Class: `PartnerRequestController`  
Endpoints: `PATCH /api/partner/requests/{requestId}/accept`, `PATCH /api/v1/partner/requests/{requestId}/accept`, `PATCH /api/partner/requests/{requestId}/reject`, `PATCH /api/v1/partner/requests/{requestId}/reject`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\controller\PartnerDispatchCompatibilityController.java`  
Class: `PartnerDispatchCompatibilityController`  
Endpoints: `POST /api/partner/dispatch/{requestId}/accept`, `POST /api/v1/partner/dispatch/{requestId}/accept`, `POST /api/partner/dispatch/{requestId}/reject`, `POST /api/v1/partner/dispatch/{requestId}/reject`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `accept`, `reject`

Evidence:
`PartnerRequestController` maps `/api/partner` and `/api/v1/partner`, exposes request list/detail and accept/reject endpoints, and calls `partnerRequestService.accept` / `partnerRequestService.reject`. `PartnerDispatchCompatibilityController` exposes compatibility dispatch accept/reject routes under the same base mappings and delegates to the same service methods.

Flow impact:
Subscription partner requests created by auto-dispatch are accepted/rejected through the common partner request path. Modern and compatibility endpoints converge at `PartnerRequestService`.

### Finding: Subscription request detection is based on both subscription ID and visit ID

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `accept`, `reject`, `isSubscriptionRequest`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\entity\PartnerServiceRequestEntity.java`  
Entity: `PartnerServiceRequestEntity`

Evidence:
`PartnerServiceRequestEntity` has nullable `subscriptionId` and `subscriptionVisitId`. `PartnerRequestService#isSubscriptionRequest` returns true only when both are non-null. Both `accept` and `reject` branch on this helper.

Flow impact:
Subscription partner flow is separated from ordinary instant/scheduled booking flow at request action time.

### Finding: Subscription request acceptance locks the request row, validates partner eligibility, and delegates actual order creation

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\repository\PartnerServiceRequestRepository.java`  
Repository: `PartnerServiceRequestRepository`  
Method: `findByIdAndPartnerIdForUpdate`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`

Evidence:
`accept` loads the request with `findByIdAndPartnerIdForUpdate`, rejects non-`PENDING` requests, expires stale requests, then for subscription requests locks the partner profile with `partnerProfileRepository.findByIdForUpdate`, locks the shift with `partnerShiftRepository.findByPartnerIdForUpdate`, calls `partnerDispatchEligibilityService.requireEligibleForNewDispatch`, and delegates to `subscriptionVisitOrders.accept(request, partnerId)`.

Flow impact:
Subscription request acceptance has row-level locking for the request plus partner/shift eligibility revalidation at the moment of acceptance.

### Finding: Subscription visit order is created only during partner request acceptance

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`

Evidence:
`SubscriptionVisitOrderService#accept` creates a new `PartnerOrder` only after it locks the visit, confirms visit/subscription match, confirms no existing `visit.bookingId`, confirms subscription status `ACTIVE`, confirms visit status `SCHEDULED` or `RESCHEDULED`, and confirms parent booking payment status `SUCCESS`. It then saves the partner order, writes customer-facing `orders` fields through a native `UPDATE`, persists one `OrderItem`, sets `visit.bookingId`, sets `visit.status = ACCEPTED`, and increments partner active job count.

Flow impact:
The authoritative normal service order for a subscription visit is created at partner acceptance, not at subscription creation or auto-dispatch request creation.

### Finding: Authoritative runtime order entity after acceptance is the normal orders/PartnerOrder row

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Methods: `arriveOrder`, `startOrder`, `requestCompletePin`, `completeOrder`

Evidence:
`SubscriptionVisitOrderService#accept` creates a `PartnerOrder`, sets `assignedPartnerId`, sets `OrderStatus.ACCEPTED`, and stores its ID in `SubscriptionVisit.bookingId`. Later partner lifecycle operations use `PartnerOrderService` and load the `PartnerOrder` row by order ID using `getOwnedOrderForUpdate`.

Flow impact:
After acceptance, arrival/start/completion operate through the same partner order lifecycle as normal bookings. `SubscriptionVisit.bookingId` is the link back to the subscription visit.

### Finding: Visit/order status synchronization is implemented from PartnerOrder to SubscriptionVisit

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Method: `saveTransition`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `syncStatus`

Evidence:
`PartnerOrderService#saveTransition` calls `subscriptionVisitOrders.syncStatus(saved)` after saving every partner order transition when the service is injected. `syncStatus` finds the visit by `bookingId` and maps partner order status to visit status: `ACCEPTED -> ACCEPTED`, `ARRIVED -> ARRIVED`, `STARTED`/`IN_PROGRESS -> IN_PROGRESS`, `COMPLETED -> COMPLETED`, and `CANCELLED -> CANCELLED`.

Flow impact:
Arrive, start, complete, and admin-complete transitions on the normal order are reflected back to the subscription visit record.

### Finding: Subscription completion is derived from all visits reaching COMPLETED

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `syncStatus`

Evidence:
When `syncStatus` maps a partner order to `VisitStatus.COMPLETED`, it loads the subscription and compares `visits.countBySubscriptionIdAndStatus(subscription.getId(), VisitStatus.COMPLETED)` to `subscription.getVisits()`. If equal, it sets the subscription status to `SubscriptionStatus.COMPLETED`.

Flow impact:
Final subscription completion is tied to completed visit count, not just payment or dispatch state.

### Finding: Partner order lifecycle endpoints are connected for subscription-created orders

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\controller\LexhoPartnerOrderController.java`  
Class: `LexhoPartnerOrderController`  
Endpoints: `POST /api/orders/{id}/arrive`, `POST /api/v1/orders/{id}/arrive`, `POST /api/orders/{id}/start`, `POST /api/v1/orders/{id}/start`, `POST /api/orders/{id}/start/verify`, `POST /api/v1/orders/{id}/start/verify`, `POST /api/orders/{id}/complete-pin`, `POST /api/v1/orders/{id}/complete-pin`, `POST /api/orders/{id}/complete`, `POST /api/v1/orders/{id}/complete`, `POST /api/orders/{id}/complete/verify`, `POST /api/v1/orders/{id}/complete/verify`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`

Evidence:
`LexhoPartnerOrderController` exposes arrive, start, start verification, complete PIN request, complete, and complete verification endpoints under `/api/orders` and `/api/v1/orders`. These delegate to `PartnerOrderService`, which locks owned orders with `getOwnedOrderForUpdate`, enforces assigned-partner ownership, validates allowed status transitions, and calls `saveTransition`.

Flow impact:
Subscription visits converted to normal `PartnerOrder` rows can progress through the existing arrival/start/complete lifecycle.

### Finding: Duplicate accept by the same partner is idempotent after visit order creation

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`

Evidence:
If `visit.getBookingId()` is already non-null, `accept` loads the existing `PartnerOrder`. If the accepting partner matches `existing.getAssignedPartnerId()`, it returns the existing order ID rather than creating another order.

Flow impact:
Repeated acceptance by the same assigned partner returns the same subscription visit order and does not duplicate the order.

### Finding: Acceptance by a different partner is blocked after a visit already has an order

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Method: `accept`

Evidence:
When `visit.bookingId` exists, `accept` loads the existing partner order and throws `BadRequestException("Visit already accepted by another partner")` if the current partner does not match `existing.getAssignedPartnerId()`.

Flow impact:
Two-partner acceptance is serialized at the visit row because `accept` locks the visit with `findByIdForUpdate`. The losing partner cannot create a second visit order after the first partner has set `visit.bookingId`.

### Finding: Peer pending subscription requests are rejected after one request accepts

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `accept`, `rejectPeerSubscriptionRequests`

Evidence:
After subscription request acceptance, `PartnerRequestService#accept` sets the accepted request's `cartId` to the visit order ID, marks it `ACCEPTED`, saves it, then calls `rejectPeerSubscriptionRequests`. That method loads pending requests by `subscriptionVisitId` and sets all other rows to `REJECTED` with reason `Accepted by another partner`.

Flow impact:
The normal path cleans up other pending requests after one partner accepts the visit.

### Finding: Partner rejection retries subscription dispatch through the same auto-dispatch service

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `reject`, `maybeContinueSubscriptionDispatch`

Evidence:
For subscription requests, `reject` marks the request `REJECTED`, saves it, and calls `maybeContinueRequestDispatch`. `maybeContinueSubscriptionDispatch` counts pending and accepted requests for the visit and calls `subscriptionAutoDispatchService.retryDispatch(subscriptionId, visitId)` only when both counts are zero.

Flow impact:
Partner rejection can continue matching for the same subscription visit, but only after there are no pending or accepted requests remaining.

### Finding: Visit assignment row is separate from the normal order assignment

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `markPartnerAccepted`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\entity\SubscriptionVisitPartnerAssignment.java`  
Entity: `SubscriptionVisitPartnerAssignment`

Evidence:
After accepting a subscription request, `PartnerRequestService#accept` calls `subscriptionAutoDispatchService.markPartnerAccepted`, which inserts a `SubscriptionVisitPartnerAssignment` row with reason `PARTNER_ACCEPTED` if one does not already exist. The actual service order assignment is stored separately in the `PartnerOrder.assignedPartnerId` field created by `SubscriptionVisitOrderService#accept`.

Flow impact:
There are two assignment records after acceptance: the normal order's assigned partner is authoritative for work execution, while `subscription_visit_partner_assignment` is an audit/admin assignment record for the visit.

### Finding: Unique constraint protects one visit assignment row, but accept path still has an insert race

CONCURRENCY RISK:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `markPartnerAccepted`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\entity\SubscriptionVisitPartnerAssignment.java`  
Entity: `SubscriptionVisitPartnerAssignment`  
Backend test file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\repository\SubscriptionAssignmentRepositoryTest.java`

Evidence:
`markPartnerAccepted` performs `findByVisitId` and then `save` without locking. The entity declares `@Column(name = "visit_id", nullable = false, unique = true)`. `SubscriptionAssignmentRepositoryTest#assignmentRepositoriesSupportLookupOrderingAndUniqueness` verifies duplicate visit assignment persistence raises `DataIntegrityViolationException`.

Flow impact:
The database prevents two rows for the same visit assignment, but concurrent accept paths could still race into a duplicate-key exception at assignment insert time unless serialized by the prior visit lock and transaction timing in practice. This should be stress-tested.

### Finding: Admin subscription-level replacement does not modify existing accepted visit orders

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\controller\AdminSubscriptionController.java`  
Class: `AdminSubscriptionController`  
Endpoint: `POST /api/admin/subscriptions/{subscriptionId}/replacement-partner`, `POST /api/v1/admin/subscriptions/{subscriptionId}/replacement-partner`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\AdminSubscriptionManagementService.java`  
Class: `AdminSubscriptionManagementService`  
Method: `replacePartner`

Evidence:
`replacePartner` validates the replacement partner's skill and dispatch eligibility, loads the existing `SubscriptionPartnerAssignment`, sets `replacementPartnerId`, sets status `REPLACED`, sets `replacedAt`, and saves the subscription-level assignment. It does not query `SubscriptionVisit`, does not update `visit.bookingId`, does not update `PartnerOrder.assignedPartnerId`, and does not cancel any already accepted visit order.

Flow impact:
Partner A can keep an already accepted normal visit order after admin replaces the subscription-level partner with Partner B. The replacement affects future preferred dispatch because `SubscriptionAutoDispatchService#preferredPartnerId` uses `replacementPartnerId`, but it does not reassign active orders already created for earlier visits.

### Finding: Admin visit-level partner assignment does not update existing normal visit order

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\controller\AdminSubscriptionController.java`  
Class: `AdminSubscriptionController`  
Endpoint: `PATCH /api/admin/subscriptions/{subscriptionId}/visits/{visitId}/assign-partner`, `PATCH /api/v1/admin/subscriptions/{subscriptionId}/visits/{visitId}/assign-partner`
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\AdminSubscriptionManagementService.java`  
Class: `AdminSubscriptionManagementService`  
Method: `assignVisitPartner`

Evidence:
`assignVisitPartner` validates the subscription, visit, partner skill, and dispatch eligibility, then creates or updates `SubscriptionVisitPartnerAssignment` for the visit. It does not inspect `visit.bookingId`, does not load the `PartnerOrder`, and does not update the normal order's `assignedPartnerId` or status.

Flow impact:
If a visit already has an accepted order for Partner A, assigning the visit partner to Partner B changes only the subscription visit assignment record. Partner A's old normal order can remain active and manageable by Partner A.

### Explicit Race: Partner A accepts, then admin/customer replaces with Partner B

PARTIAL:

Evidence path:
1. Partner A accepts a subscription visit request through `PartnerRequestService#accept`.
2. `SubscriptionVisitOrderService#accept` creates a normal `PartnerOrder` assigned to Partner A and sets `SubscriptionVisit.bookingId`.
3. `SubscriptionAutoDispatchService#markPartnerAccepted` creates a visit assignment row for Partner A.
4. Admin calls `AdminSubscriptionManagementService#replacePartner`, which updates only the subscription-level replacement partner.
5. Or admin calls `AdminSubscriptionManagementService#assignVisitPartner`, which updates only `subscription_visit_partner_assignment`.

Flow impact:
Code evidence shows no automatic cancellation, reassignment, or invalidation of Partner A's existing `PartnerOrder`. Partner A can remain the authoritative assignee for the existing order because `PartnerOrderService#getOwnedOrderForUpdate` authorizes lifecycle actions from `PartnerOrder.assignedPartnerId`.

### Finding: PartnerOrder ownership check blocks non-assigned partner lifecycle actions

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Methods: `getOwnedOrderForUpdate`, `arriveOrder`, `startOrder`, `completeOrder`

Evidence:
`getOwnedOrderForUpdate` loads the partner order with `findByIdForUpdate` and checks `partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)`. Arrive, start, complete PIN, and complete all call `getOwnedOrderForUpdate`.

Flow impact:
Partner B cannot operate on Partner A's old accepted order unless the normal order's `assignedPartnerId` is changed. Since replacement paths do not change it, the old order remains under Partner A.

### Finding: Start and completion still require customer PIN verification

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Methods: `arriveOrder`, `startOrder`, `requestCompletePin`, `completeOrder`

Evidence:
`arriveOrder` generates a start PIN. `startOrder(partnerId, orderId, beforeImage, pin)` requires order status `ARRIVED` and calls `verifyPin` for `PinPurpose.START`. The no-PIN overload throws `BadRequestException("Customer start PIN verification is required")`. `requestCompletePin` requires `IN_PROGRESS`, and `completeOrder` requires `IN_PROGRESS`, requires consumed start PIN, verifies completion PIN, then transitions to `COMPLETED`.

Flow impact:
Subscription visit orders use the same start and completion verification constraints as normal partner orders.

### Finding: Active job count increments on subscription accept and decrements through sync/normal completion

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionVisitOrderService.java`  
Class: `SubscriptionVisitOrderService`  
Methods: `accept`, `syncStatus`  
Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexhopartner\service\PartnerOrderService.java`  
Class: `PartnerOrderService`  
Method: `saveTransition`

Evidence:
`SubscriptionVisitOrderService#accept` increments `shift.activeJobsCount` for the assigned partner. `syncStatus` decrements active jobs on `VisitStatus.COMPLETED` or `CANCELLED`. `PartnerOrderService#saveTransition` also calls `decrementActiveJobs` when the partner order transitions to `COMPLETED`.

Flow impact:
Completion may decrement active jobs through both `syncStatus` and `PartnerOrderService#decrementActiveJobs` in the same completion transaction. Both use `Math.max(0, count - 1)`, preventing negative counts, but this can undercount active jobs if a partner has multiple active jobs. This should be reviewed in Phase 14 concurrency/state accounting.

### Finding: Tests cover subscription request accept routing, retry on rejection, admin replacement prerequisites, and assignment uniqueness

PASS:

Backend test files:  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\partnerrequest\service\PartnerRequestServiceTest.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\service\AdminSubscriptionManagementServiceTest.java`  
`C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\repository\SubscriptionAssignmentRepositoryTest.java`

Evidence:
`PartnerRequestServiceTest#acceptShouldCompleteSubscriptionRequestAndRejectPeers` verifies subscription accept calls `markPartnerAccepted` and not the instant dispatch acceptance path. `PartnerRequestServiceTest#rejectShouldRetrySubscriptionDispatchWhenNoPendingRequestsRemain` verifies subscription rejection calls `retryDispatch`. `AdminSubscriptionManagementServiceTest#replacePartnerShouldRequireExistingAssignment` verifies replacement requires an existing assignment. `SubscriptionAssignmentRepositoryTest#assignmentRepositoriesSupportLookupOrderingAndUniqueness` verifies uniqueness for subscription and visit assignments.

Flow impact:
Key routing and assignment persistence behaviors have unit/integration coverage. Phase 7 did not find focused tests for `SubscriptionVisitOrderService#accept`, two concurrent subscription accepts, or replacement after an order is already created.

### Phase 7 Interim Verdict

PARTIAL:

Verified connected:
- Partner request list/detail/accept/reject endpoints are connected through `/api/partner`, `/api/v1/partner`, and compatibility `/dispatch` routes.
- Subscription requests are identified by `subscriptionId` plus `subscriptionVisitId`.
- Subscription partner acceptance creates the normal service order via `SubscriptionVisitOrderService#accept`.
- Actual order creation locks the visit, checks active subscription, checks visit state, checks parent payment success, creates `PartnerOrder`, stores `visit.bookingId`, and marks the visit `ACCEPTED`.
- Partner lifecycle endpoints for arrive/start/complete are connected and sync `PartnerOrder` status back to `SubscriptionVisit`.
- All completed visits mark the subscription `COMPLETED`.
- Duplicate accept by the same partner returns the existing order ID.
- Second-partner accept after `visit.bookingId` exists is blocked.
- Rejection/expiry can retry dispatch.

Risks / gaps:
- `SubscriptionVisitPartnerAssignment` and `PartnerOrder.assignedPartnerId` are separate; the normal order is authoritative for actual work.
- Admin subscription-level replacement only changes future preferred assignment and does not update/cancel existing accepted visit orders.
- Admin visit-level assignment only changes the assignment table and does not update/cancel an existing normal `PartnerOrder`.
- Explicit Partner A accepted -> Partner B replacement scenario leaves Partner A's existing order active unless another path manually changes/cancels it.
- Visit assignment insert has unique DB protection but a read-check-write race remains in `markPartnerAccepted`.
- Active job count may be decremented twice on completion because both `syncStatus` and `PartnerOrderService#saveTransition` decrement.
- No focused tests found for `SubscriptionVisitOrderService#accept`, concurrent subscription accept, or post-accept replacement behavior.

PHASE 7 COMPLETE

## 9. Skill Matching Audit

### Phase 8 Scope

Phase 8 traced partner skill matching for:

- Instant booking dispatch
- Scheduled booking dispatch
- Subscription visit auto-dispatch
- Admin dispatch/re-dispatch
- Subscription admin assignment/replacement
- Partner request acceptance
- Customer and partner mobile API participation

Primary skill persistence and lookup:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\entity\PartnerServiceSkillEntity.java`  
Class: `PartnerServiceSkillEntity`  
Entity/table: `partner_service`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\repository\PartnerServiceSkillRepository.java`  
Repository: `PartnerServiceSkillRepository`  
Methods:
- `existsByPartnerIdAndSubServiceId(Long partnerId, Long subServiceId)`
- `existsByPartnerIdAndCategoryId(Long partnerId, Long categoryId)`
- `findByPartnerId(Long partnerId)`
- `deleteByPartnerId(Long partnerId)`

Evidence:
`PartnerServiceSkillEntity` maps `partner_id` and `sub_service_id` to table `partner_service`. `PartnerServiceSkillRepository#existsByPartnerIdAndCategoryId` joins `PartnerServiceSkillEntity` to `AdminServiceEntity` and checks `s.category.id = :categoryId`.

Flow impact:
The canonical new skill mapping is the `partner_service` table. Normal dispatch can also accept legacy partner category/subcategory fields before consulting `partner_service`.

### Finding: Partner skill management endpoint writes `partner_service`

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\controller\PartnerDispatchController.java`  
Class: `PartnerDispatchController`  
Methods/endpoints:
- `upsertSkills`, `PUT /api/partner/dispatch/skills` and inherited controller prefix variants
- `getSkills`, `GET /api/partner/dispatch/skills` and inherited controller prefix variants

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\PartnerSkillService.java`  
Class: `PartnerSkillService`  
Methods: `upsertSkills`, `getSkills`

Evidence:
`PartnerDispatchController#upsertSkills` delegates to `partnerSkillService.upsertSkills(partnerId, request)`. `PartnerSkillService#upsertSkills` resolves `subServiceIds` directly and expands `categoryIds` through `adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(categoryId)`, deletes existing `partner_service` rows for the partner, saves one `PartnerServiceSkillEntity` per resolved sub-service, and sets legacy `Partner.subCategoryId` only if it was previously null. `getSkills` reads `partnerServiceSkillRepository.findByPartnerId(partnerId)`.

Partner app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`  
Methods: `getPartnerSkills`, `updatePartnerSkills`

Evidence:
The partner app calls `GET /api/v1/partner/dispatch/skills` and `PUT /api/v1/partner/dispatch/skills`, mapping `categoryIds` and `subServiceIds`.

Flow impact:
Partners can manage their skill mappings from the partner app, and those mappings are the same rows used by dispatch filtering.

### Finding: Customer booking creation sends service ID, not partner skill data

PASS:

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\types\contracts.ts`  
Type: `CreateBookingRequest`

Customer app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile\src\api\bookingApi.ts`  
Method: `createBooking`

Evidence:
`CreateBookingRequest` contains `bookingType`, `serviceId`, `addressId`, date/slot, coordinates, note, and coupon fields. `createBooking` sends `serviceId: Number(payload.serviceId ?? 0)` to `/api/v1/bookings/create`, with fallback to `/api/bookings/create`.

Flow impact:
Customer app does not select or verify partner skills. It passes a requested service ID; backend dispatch resolves the required skill from the service ID.

### Finding: Instant automatic dispatch filters by required skill before creating partner requests

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\lexho\controller\LexhoExperienceController.java`  
Class: `LexhoExperienceController`  
Method: `dispatchInstantBooking`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `dispatchOrder`, `dispatchOrderInternal`, `findEligibleCandidates`, `isEligiblePartner`, `revalidateCandidateForNewDispatch`, `hasRequiredSkill`, `createPartnerRequests`

Evidence:
`LexhoExperienceController#dispatchInstantBooking` calls `dispatchOrchestrationService.dispatchOrder(order.getId(), userId, addressSelection.dispatchAddressId(), request.serviceId(), 1, placeOrderRequest.getScheduledAt())`. `DispatchOrchestrationService#buildDispatchContext` resolves the requested service to `resolvedSubServiceId` and `requestedCategoryId`. `isEligiblePartner` calls `hasRequiredSkill(partner, context.resolvedSubServiceId(), context.requestedCategoryId())`; a false result records `DISPATCH_REJECTED` with reason `SKILL_MISMATCH`. `createPartnerRequests` revalidates each candidate under `revalidateCandidateForNewDispatch`; that method again requires `hasRequiredSkill(...)` before saving a `PartnerServiceRequestEntity`.

Skill matching logic:
- Legacy direct sub-service match: `partner.getSubCategoryId().equals(requestedSubServiceId)`
- Legacy category match: `partner.getCategoryId().equals(requestedCategoryId)`
- New skill row match: `partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(...)`
- Category fallback through skill rows: `existsByPartnerIdAndCategoryId(...)`

Flow impact:
Instant automatic dispatch does not create partner requests for candidates that fail skill matching at initial filter or locked revalidation time.

### Finding: Scheduled dispatch uses the same dispatch skill filter when dispatched through orchestration

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `dispatchOrder`, `dispatchOrderInternal`, `shouldDeferDispatch`, `findEligibleCandidates`, `hasRequiredSkill`

Evidence:
`dispatchOrderInternal` builds a `DispatchContext` with `scheduled = BookingType.SCHEDULED.name().equalsIgnoreCase(order.getBookingType())`. If the scheduled dispatch window has not arrived, `shouldDeferDispatch` records `DISPATCH_DEFERRED`; once active, the flow continues to `startPartnerSearch`, `dispatchAttemptsForOrder`, `findEligibleCandidates`, and the same `hasRequiredSkill` checks used for instant dispatch.

Flow impact:
Scheduled booking dispatch enforces the same skill filter as instant dispatch once the scheduled dispatch window is reached.

### Finding: Admin dispatch/re-dispatch of normal orders uses the same skill filter

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\controller\AdminDispatchController.java`  
Class: `AdminDispatchController`  
Endpoint: `POST /api/admin/dispatch/orders/{orderId}`, `POST /api/v1/admin/dispatch/orders/{orderId}`  
Method: `dispatchOrder`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Methods: `dispatchOrder`, `retryDispatch`, `findEligibleCandidates`, `hasRequiredSkill`

Evidence:
`AdminDispatchController#dispatchOrder` loads the order and calls `dispatchOrchestrationService.dispatchOrder(...)` with the request's `serviceId`. `PartnerRequestService#maybeContinueDispatch` calls `dispatchOrchestrationService.retryDispatch(orderId)` after all pending requests are gone; `retryDispatch` rebuilds a `DispatchContext` from the dispatch snapshot's `serviceId` and calls `findEligibleCandidates`, which calls `hasRequiredSkill`.

Flow impact:
Admin-triggered dispatch and automatic retry/redispatch for normal instant/scheduled orders use the same skill checks as customer-triggered dispatch.

### Finding: Subscription auto-dispatch requires exact `partner_service` sub-service skill

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Methods: `autoAssignDueVisits`, `autoAssignVisit`, `dispatchForBooking`, `retryDispatch`, `createDispatchRequests`, `isEligiblePartner`

Evidence:
`createDispatchRequests` first checks a preferred partner from `SubscriptionPartnerAssignment`; if absent or ineligible, it searches nearest eligible partners. Both preferred and fallback paths call `isEligiblePartner`. `isEligiblePartner` requires profile and shift eligibility, then rejects the partner unless `partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, subscription.getServiceId())` returns true. It then validates live location, max dispatch radius, and ETA.

Flow impact:
Subscription visit auto-dispatch enforces exact partner/sub-service mapping in `partner_service`. Unlike normal dispatch, it does not treat legacy `Partner.subCategoryId` or category-level legacy fields as sufficient.

### Finding: Subscription retry after rejection also enforces exact sub-service skill

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Methods: `reject`, `maybeContinueRequestDispatch`, `maybeContinueSubscriptionDispatch`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchService.java`  
Class: `SubscriptionAutoDispatchService`  
Method: `retryDispatch`

Evidence:
`PartnerRequestService#maybeContinueSubscriptionDispatch` counts pending and accepted requests for a subscription visit and calls `subscriptionAutoDispatchService.retryDispatch(subscriptionId, visitId)` only when both are zero. `SubscriptionAutoDispatchService#retryDispatch` checks no existing visit assignment or active request, loads the subscription and visit, and calls `createDispatchRequests`, which calls `isEligiblePartner` and the exact `existsByPartnerIdAndSubServiceId` skill check.

Flow impact:
Subscription redispatch/retry after rejection does not bypass skill matching.

### Finding: Subscription admin assignment and replacement validate exact skill mapping

PASS:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\controller\AdminSubscriptionController.java`  
Class: `AdminSubscriptionController`  
Endpoints:
- `PATCH /api/admin/subscriptions/{subscriptionId}/assign-partner`, `PATCH /api/v1/admin/subscriptions/{subscriptionId}/assign-partner`
- `PATCH /api/admin/subscriptions/{subscriptionId}/replace-partner`, `PATCH /api/v1/admin/subscriptions/{subscriptionId}/replace-partner`
- `PATCH /api/admin/subscriptions/{subscriptionId}/visits/{visitId}/assign-partner`, `PATCH /api/v1/admin/subscriptions/{subscriptionId}/visits/{visitId}/assign-partner`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\subscription\service\AdminSubscriptionManagementService.java`  
Class: `AdminSubscriptionManagementService`  
Methods: `assignPartner`, `replacePartner`, `assignVisitPartner`, `validatePartnerSkill`, `validatePartnerDispatchEligibility`

Evidence:
`assignPartner`, `replacePartner`, and `assignVisitPartner` all call `validatePartnerSkill(partner.getId(), subscription.getServiceId())` before writing assignment rows. `validatePartnerSkill` throws unless `partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, serviceId)` is true. They also call `validatePartnerDispatchEligibility`, which checks address coordinates, shift, partner eligibility, live location, max radius, and ETA.

Flow impact:
Subscription admin assignment/replacement is not a skill bypass. It requires exact `partner_service` mapping for the subscription service. However, as recorded in Phase 7, replacement still does not update or cancel an already accepted normal `PartnerOrder`.

### Finding: Normal booking admin management does not expose a direct partner assignment endpoint in inspected controller

UNKNOWN — CODE EVIDENCE INSUFFICIENT:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\admin\controller\AdminBookingManagementController.java`  
Class: `AdminBookingManagementController`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\admin\service\AdminBookingManagementService.java`  
Class: `AdminBookingManagementService`

Evidence:
`AdminBookingManagementController` exposes list, by-id, status update, cancel, live tracking, navigation, partner-location override, payment reconciliation/status, and payout endpoints. No direct endpoint assigning a normal instant/scheduled order to a specific partner was found in this controller. `AdminDispatchController` can dispatch an order through orchestration, which does enforce skill matching.

Flow impact:
For normal instant/scheduled bookings, Phase 8 verified admin dispatch skill matching, but did not verify a normal admin manual partner assignment path because no direct normal-order assignment endpoint was found in the inspected admin booking controller. Continue checking in Phase 10 admin assignment.

### Finding: Partner acceptance does not revalidate service skill

PARTIAL:

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\partnerrequest\service\PartnerRequestService.java`  
Class: `PartnerRequestService`  
Method: `accept`

Backend file: `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\main\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationService.java`  
Class: `DispatchOrchestrationService`  
Method: `markPartnerAccepted`

Evidence:
For normal requests, `PartnerRequestService#accept` locks the request, checks pending/expiry/order/payment/ETA, then calls `partnerMarketplaceService.require(partnerId)` and `partnerDispatchEligibilityService.requireEligibleForNewDispatch(...)`. It does not call `PartnerServiceSkillRepository` or `hasRequiredSkill`. `DispatchOrchestrationService#markPartnerAccepted` locks the order and shift, checks `canReceiveNewBookings`, assigns `order.assignedPartnerId`, and increments active jobs; it also does not recheck skill.

For subscription requests, `PartnerRequestService#accept` checks partner dispatch eligibility and calls `SubscriptionVisitOrderService#accept`, then `subscriptionAutoDispatchService.markPartnerAccepted`; no skill check is performed during accept.

Flow impact:
Skill is enforced when requests are created, not when they are accepted. If a partner's skill mapping changes after request creation but before acceptance, existing pending requests can still be accepted because accept only validates request ownership, status, order/payment state, ETA, and dispatch eligibility. Classification is PARTIAL because the normal expected path is skill-filtered at dispatch time, but accept-time drift is not revalidated.

### Finding: Partner app acceptance paths rely on backend request matching

PASS:

Partner app file: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile\src\services\partnerService.ts`  
Methods: `acceptDispatchRequest`, `acceptPartnerOrder`, `acceptPartnerRequest`, `rejectDispatchRequest`

Evidence:
`acceptDispatchRequest` tries `/api/v1/partner/requests/{requestId}/accept`, fallback `/api/partner/requests/{requestId}/accept`, subscription accept fallbacks, and legacy order/dispatch accept routes. `acceptPartnerOrder` calls `/api/v1/orders/{orderId}/accept` then `/api/orders/{orderId}/accept`. No frontend-side skill filter is applied when accepting.

Flow impact:
Partner app does not enforce skill locally. It depends on backend dispatch to only send eligible requests and backend accept to authorize request ownership/status.

### Finding: Tests cover normal dispatch skill filtering and subscription/admin skill validation

PASS:

Backend test files:
- `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\dispatch\service\DispatchOrchestrationSkillFilterTest.java`
- `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\dispatch\service\PartnerSkillServiceTest.java`
- `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\service\SubscriptionAutoDispatchServiceTest.java`
- `C:\Users\shivendra\Downloads\lexho-user-service-master\application-module\src\test\java\com\lexho\user_service\subscription\service\AdminSubscriptionManagementServiceTest.java`

Evidence:
`DispatchOrchestrationSkillFilterTest` stubs `existsByPartnerIdAndSubServiceId` and `existsByPartnerIdAndCategoryId` to verify only skill-matched partners receive dispatch. `PartnerSkillServiceTest` verifies skill upsert/get behavior. `SubscriptionAutoDispatchServiceTest` stubs exact `existsByPartnerIdAndSubServiceId` for subscription dispatch. `AdminSubscriptionManagementServiceTest` verifies unskilled partners are rejected and skilled partners can be assigned.

Flow impact:
Focused tests exist for normal dispatch skill filtering, partner skill persistence, subscription dispatch skill matching, and subscription admin assignment skill validation. No focused test was found in Phase 8 for skill removal after request creation followed by partner accept.

### Phase 8 Bypass / Gap Inventory

PASS:
- Instant automatic dispatch: skill enforced through `DispatchOrchestrationService#hasRequiredSkill`.
- Scheduled dispatch: skill enforced through the same orchestration path when dispatch occurs.
- Admin dispatch/re-dispatch for normal orders: skill enforced because `AdminDispatchController` and retry paths call `DispatchOrchestrationService`.
- Subscription auto-dispatch: exact `partner_service` sub-service skill enforced in `SubscriptionAutoDispatchService#isEligiblePartner`.
- Subscription retry/redispatch after rejection: exact skill enforced via `SubscriptionAutoDispatchService#retryDispatch`.
- Subscription admin assignment/replacement/visit assignment: exact skill enforced via `AdminSubscriptionManagementService#validatePartnerSkill`.

PARTIAL:
- Partner accept: no accept-time skill revalidation. Existing pending requests can be accepted even if skill mapping changes after dispatch.
- Subscription replacement: skill is validated for the replacement assignment, but it does not update/cancel an existing accepted normal `PartnerOrder` from the previous partner, as recorded in Phase 7.

UNKNOWN — CODE EVIDENCE INSUFFICIENT:
- Direct normal instant/scheduled admin manual assignment to a specific partner was not found in `AdminBookingManagementController` or `AdminBookingManagementService`. Phase 10 should continue searching all admin routes for any other normal-order assignment endpoint.

### Phase 8 Interim Verdict

PARTIAL:

The primary dispatch paths are skill-aware. Normal instant/scheduled dispatch uses legacy partner subcategory/category fields plus `partner_service` rows. Subscription dispatch and subscription admin assignment are stricter and require exact `partner_service` sub-service rows. Admin dispatch and retry/redispatch paths reuse the same backend matching services, so no skill bypass was found there.

The main gap is accept-time drift: once a `PartnerServiceRequestEntity` exists, partner acceptance does not revalidate skill mapping. This means skill changes after request creation do not invalidate pending requests. The second gap is already covered by Phase 7: subscription replacement validates the new partner's skill but does not repair an already accepted old partner order.

PHASE 8 COMPLETE

## 10. Schedule Conflict Audit

## 11. Lifecycle State Machine Audit

## 12. Idempotency Audit

## 13. Concurrency Audit

## 14. Admin Assignment Audit

## 15. Customer History Audit

## 16. Partner App API Audit

## 17. Error Handling Audit

## 18. Broken / Missing Connections

## 19. Critical Findings

## 20. Recommended Production Fixes

## 21. Verification Test Cases

## 22. Final E2E Traces

## 23. Final Verdict
