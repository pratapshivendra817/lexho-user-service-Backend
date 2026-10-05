# Lexho Booking Flows README

Source-backed end-to-end flow for:

- Backend: `C:\Users\shivendra\Downloads\lexho-user-service-master`
- Customer app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\customer\legacy-mobile`
- Partner app: `C:\Users\shivendra\lexho-frontend-monorepo\apps\partner\legacy-mobile`

This file covers three customer booking modes:

- `INSTANT`
- `SCHEDULED`
- `SUBSCRIPTION`

## 1. High Level Architecture

Customer app me booking ka main entry point:

- `src/screens/cart/CartCrudScreen.tsx`

Customer app APIs:

- `src/api/bookingApi.ts`
- `src/api/paymentApi.ts`
- `src/api/subscriptionBookingApi.ts`
- `src/api/subscriptionApi.ts`
- `src/api/subscriptionVisitsApi.ts`

Backend booking/order entry points:

- `LexhoExperienceController#createBooking`
- `OrderService#placeOrder`
- `DispatchOrchestrationService`
- `PartnerRequestService`
- `SubscriptionController`
- `SubscriptionBookingService`
- `SubscriptionAutoDispatchService`
- `SubscriptionVisitOrderService`
- `LexhoPartnerOrderController`
- `PartnerOrderService`

Partner app entry points:

- `src/services/partnerService.ts`
- `src/screens/main/NewOrdersScreen.tsx`
- `src/screens/main/ActiveJobScreen.tsx`
- `src/screens/main/OrderHistoryScreen.tsx`
- `src/hooks/useRealtimeDispatchRequests.ts`
- `src/features/shift/locationTrackingManager.ts`

## 2. Common Concepts

Normal booking means instant and scheduled booking. Dono customer app se same booking API par jaate hain:

- Primary: `POST /api/v1/bookings/create`
- Fallback: `POST /api/bookings/create`

Subscription booking alag API use karta hai:

- Primary: `POST /api/subscription/create`
- Fallback alias: `POST /api/subscription/bookings`

Payment APIs:

- `POST /api/payments/create-order`
- `POST /api/payments/verify`
- Failure fallback attempts:
  - `POST /api/payments/failure`
  - `POST /api/payments/payment-failed`
  - `POST /api/payment/failure`

Customer history:

- `GET /api/bookings/history`

Partner request/order APIs:

- `GET /api/v1/partner/requests`
- `GET /api/v1/orders/new`
- `PATCH /api/v1/partner/requests/{requestId}/accept`
- `PATCH /api/v1/partner/requests/{requestId}/reject`
- `POST /api/v1/orders/{id}/accept`
- `POST /api/v1/orders/{id}/reject`
- `POST /api/v1/orders/{id}/arrive`
- `POST /api/v1/orders/{id}/start`
- `POST /api/v1/orders/{id}/start/verify`
- `POST /api/v1/orders/{id}/complete-pin`
- `POST /api/v1/orders/{id}/complete`
- `POST /api/v1/orders/{id}/complete/verify`
- `GET /api/v1/orders/history`
- `GET /api/v1/orders/{id}/timeline`

Partner app mostly `/api/v1/...` call karta hai, aur missing endpoint par `/api/...` fallback try karta hai.

## 3. Instant Booking Flow

### 3.1 Customer App Flow

Entry file:

- `src/screens/cart/CartCrudScreen.tsx`

API file:

- `src/api/bookingApi.ts`

Flow:

1. Customer cart me service add karta hai.
2. Customer booking mode `INSTANT` select karta hai.
3. Agar selected scheduled time nahi hai, app instant window ke hisab se time banata hai:
   - `getInstantIsoTime(cartPolicy.instantBookingWindowMinutes)`
4. App cart/address validation karta hai:
   - cart empty nahi hona chahiye
   - selected address hona chahiye
   - address me latitude/longitude hona chahiye
   - service id valid hona chahiye
5. App slot/date normalize karta hai.
6. App `createBooking()` call karta hai.
7. `createBooking()` first `POST /api/v1/bookings/create` call karta hai.
8. Agar backend 404/405 de, tab fallback `POST /api/bookings/create` call hota hai.
9. Network/offline error par request offline queue me store hoti hai.
10. Booking response me booking id milne ke baad app cart lock karta hai:
    - `lockCartUntilPayment({ bookingId })`
11. App `Payment` screen par navigate karta hai.
12. Payment success ke baad app `BookingPaymentConfirm` / status flow par jaata hai.

Instant booking payload shape:

```json
{
  "serviceId": 123,
  "addressId": "address-id-or-number",
  "latitude": 12.34,
  "longitude": 77.12,
  "coordinates": {
    "lat": 12.34,
    "lng": 77.12
  },
  "bookingType": "INSTANT",
  "date": "YYYY-MM-DD",
  "slot": "HH:mm-HH:mm",
  "note": "optional note",
  "couponCode": "OPTIONAL"
}
```

### 3.2 Backend Booking Create Flow

Entry file:

- `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`

Method:

- `createBooking`

Backend endpoint mapping:

- `@PostMapping({"/api/bookings/create", "/api/v1/bookings/create"})`

Flow:

1. Backend `booking_create` rate limit apply karta hai.
2. Request se customer id auth context se aata hai.
3. Address validate hota hai:
   - UUID customer location supported hai.
   - Legacy numeric address supported hai.
   - Address customer-owned hona chahiye.
   - Customer location mirror karke `LexhoAddress` compatible dispatch address ban sakta hai.
4. Geo fence check hota hai.
5. Customer serviceability check hota hai.
6. Booking type resolve hota hai:
   - explicit `INSTANT` allowed hai.
   - blank booking type default `SCHEDULED` ho sakta hai.
7. Cart config flags check hote hain:
   - instant booking enabled?
   - scheduled booking enabled?
8. Controller `PlaceOrderRequest` banata hai:
   - `bookingType = INSTANT`
   - `scheduledAt = now()` for instant
   - `dispatchAddressId`
   - `dispatchServiceId`
   - `dispatchQuantity`
   - `expectedTotal`
   - `couponCode`
9. `OrderService.placeOrder()` call hota hai.

### 3.3 OrderService Flow

Entry file:

- `application-module/src/main/java/com/lexho/user_service/service/OrderService.java`

Method:

- `placeOrder`

Flow:

1. Legacy cart rows read hote hain:
   - `cartRepository.findByUserId(userId)`
2. Agar legacy cart empty hai, new customer cart rows read hote hain:
   - `customerCartItemRepository.findByUserId(userId)`
3. Agar cart empty hai, backend `CART_EMPTY` validation error deta hai.
4. `Order` entity create hoti hai:
   - `status = CREATED`
   - `paymentStatus = PENDING`
   - `bookingType = INSTANT`
   - `scheduledAt`
   - `dispatchAddressId`
   - `dispatchServiceId`
   - `dispatchQuantity`
5. Cart item se `OrderItem` rows banti hain.
6. Add-ons selected hain to `OrderAddon` rows banti hain.
7. `expectedTotal` sent hai to backend price compare karta hai.
8. Coupon apply hota hai:
   - `couponService.applyToOrder(...)`
9. Order save hota hai.
10. Order add-ons save hote hain.
11. Customer ko booking notification send karne ki best-effort try hoti hai.
12. Cart clear hota hai:
    - legacy cart delete
    - customer cart delete
    - cart item add-ons delete
13. `OrderResponseDTO` return hota hai.

### 3.4 Instant Payment Flow

Customer file:

- `src/api/paymentApi.ts`

Flow:

1. Customer app payment screen par `createPaymentOrder()` call karta hai.
2. API call:
   - `POST /api/payments/create-order`
3. Request me `Idempotency-Key` header hota hai.
4. Backend payment service Razorpay order create/reuse karta hai.
5. App Razorpay payment flow complete karta hai.
6. App `verifyPayment()` call karta hai:
   - `POST /api/payments/verify`
7. Payment captured hone par backend `PaymentCapturedEvent` publish karta hai.
8. `InstantBookingPaymentDispatchListener#onPaymentCaptured` AFTER_COMMIT run hota hai.
9. Listener call karta hai:
   - `dispatchPaidInstantBooking(bookingId)`
   - `dispatchPaidScheduledBooking(bookingId)`
10. Instant booking paid hai to dispatch start hota hai.

Payment failed case:

1. App failure endpoint call kar sakta hai.
2. Backend `PaymentFailedEvent` handle karta hai.
3. `DispatchOrchestrationService#handlePaymentFailed` pending requests expire karta hai.
4. Instant booking cancel ho sakti hai agar service already start/complete nahi hui.

### 3.5 Instant Dispatch Flow

Entry file:

- `application-module/src/main/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationService.java`

Important methods:

- `dispatchPaidInstantBooking`
- `dispatchOrderInternal`
- `findEligibleCandidates`
- `hasRequiredSkill`
- `createPartnerRequests`
- `expireStuckInstantSearches`

Flow:

1. Payment success ke baad `dispatchPaidInstantBooking(orderId)` call hota hai.
2. Backend checks:
   - booking type `INSTANT` hai
   - payment status `SUCCESS` hai
   - dispatch already start nahi hua
   - assigned partner already nahi hai
   - dispatch metadata present hai
3. Order par `dispatchStartedAt` set hota hai.
4. `dispatchOrderInternal` call hota hai.
5. Dispatch context build hota hai:
   - order
   - customer
   - address
   - requested service
   - pincode
   - resolved sub-service id
   - category id
6. Serviceable pincode check hota hai.
7. Payment gate dobara check hota hai:
   - instant/scheduled orders payment success ke bina dispatch nahi hote.
8. Order status partner search me move hota hai:
   - instant: `SEARCHING_PARTNER`
9. Nearby partners find hote hain:
   - current partner location
   - radius
   - online shift
   - max active jobs
   - leave / availability
   - pincode/radius/ETA
   - partner skill
10. Skill matching:
    - legacy direct sub-service match
    - legacy category match
    - `partner_service` sub-service row
    - `partner_service` category row
11. Eligible candidates scoring ke baad sorted hote hain.
12. Partner service requests create hote hain:
    - status `PENDING`
    - expiry window approx 2 minutes (`PARTNER_RESPONSE_WINDOW_MINUTES`)
13. Partner app realtime/polling se request dekhta hai.
14. Agar initial radius me partner nahi mila, max radius tak expand hota hai.
15. Agar no eligible partner, booking events me failure reason store hota hai.
16. Stuck instant searches scheduler baad me timeout kar sakta hai:
    - pending requests expire
    - order status `INSTANT_FAILED`
    - unavailable reason `SEARCH_TIMEOUT`

### 3.6 Partner App Instant Accept Flow

Partner app file:

- `src/services/partnerService.ts`

Functions:

- `getNewDispatchRequests`
- `getNewOrders`
- `acceptDispatchRequest`
- `rejectDispatchRequest`

Flow:

1. Partner app new requests fetch karta hai:
   - `GET /api/v1/partner/requests?status=PENDING`
   - fallback `GET /api/v1/orders/new`
   - fallback `/api/partner/requests`, `/api/orders/new`, etc.
2. Partner accept karta hai:
   - first `PATCH /api/v1/partner/requests/{requestId}/accept`
   - fallback `PATCH /api/partner/requests/{requestId}/accept`
   - subscription accept fallbacks
   - legacy order accept fallbacks
3. Backend `PartnerRequestService#accept` request lock karta hai.
4. Backend checks:
   - request pending hai
   - request expired nahi hai
   - order payment success hai
   - order assignment-accepting state me hai
   - instant ETA max allowed se zyada nahi hai
   - partner dispatch eligible hai
5. Request `ACCEPTED` hoti hai.
6. Same booking ke peer pending requests reject hote hain.
7. `DispatchOrchestrationService#markPartnerAccepted` order assign karta hai.
8. Order status partner assigned / accepted path me move hota hai.
9. Partner active jobs count increment hota hai.

Reject / expire flow:

1. Partner reject kare to request `REJECTED`.
2. Request expire ho to request `EXPIRED`.
3. Agar no pending/accepted request bachi, backend `retryDispatch(orderId)` try karta hai.
4. Agar instant retry bhi fail kare, order `INSTANT_FAILED` ho sakta hai.

### 3.7 Partner Job Lifecycle

Backend controller:

- `LexhoPartnerOrderController`

Backend service:

- `PartnerOrderService`

Partner app functions:

- `markOrderArrived`
- `startOrder`
- `verifyOrderStartPin`
- `requestOrderCompletePin`
- `verifyOrderCompletePin`
- `completeOrder`

Lifecycle:

1. Partner accepts request/order.
2. Partner job active screen par order open karta hai.
3. Partner arrival mark karta hai:
   - `POST /api/v1/orders/{id}/arrive`
   - fallback `POST /api/orders/{id}/arrive`
4. Backend `arriveOrder` order status `ARRIVED` karta hai.
5. Backend customer start PIN generate karta hai:
   - 4 digit PIN
   - nonce based
   - expiry approx 15 minutes
   - max attempts 5
6. Partner start service karta hai:
   - `POST /api/v1/orders/{id}/start`
   - optional `beforeImage`
   - `pin` request param supported
7. Partner app currently PIN verify route bhi use karta hai:
   - `POST /api/orders/{id}/start/verify`
8. Backend PIN verify karta hai.
9. PIN valid hai to order `IN_PROGRESS` hota hai.
10. Partner completion PIN request karta hai:
    - `POST /api/orders/{id}/complete-pin`
11. Backend complete PIN generate/refresh karta hai.
12. Partner complete service karta hai:
    - `POST /api/v1/orders/{id}/complete`
    - optional `afterImage`
    - `pin` request param supported
13. Partner app complete verify route bhi use karta hai:
    - `POST /api/orders/{id}/complete/verify`
14. Backend complete PIN verify karta hai.
15. Order `COMPLETED` hota hai.
16. Completion par:
    - order completed metric increment
    - partner payout ensure hota hai
    - active jobs count decrement hota hai
    - customer/partner notifications fire ho sakti hain
    - status history row save hoti hai

## 4. Scheduled Booking Flow

### 4.1 Customer App Flow

Entry file:

- `src/screens/cart/CartCrudScreen.tsx`

API file:

- `src/api/bookingApi.ts`

Flow:

1. Customer booking mode `SCHEDULED` select karta hai.
2. Scheduled booking ke liye explicit date/time slot required hai.
3. Agar slot missing hai, app time picker open karta hai.
4. App address, coordinates, service id, slot validate karta hai.
5. App `createBooking()` call karta hai.
6. API same hai:
   - `POST /api/v1/bookings/create`
   - fallback `POST /api/bookings/create`
7. Scheduled payload:

```json
{
  "serviceId": 123,
  "addressId": "address-id-or-number",
  "latitude": 12.34,
  "longitude": 77.12,
  "bookingType": "SCHEDULED",
  "date": "YYYY-MM-DD",
  "slot": "HH:mm-HH:mm",
  "note": "optional note",
  "couponCode": "OPTIONAL"
}
```

8. App behavior currently do variants support karta hai:
   - payment flow: booking create -> cart lock -> `Payment`
   - scheduled status flow: booking create -> cart refresh -> `ScheduledBookingStatus`

### 4.2 Backend Scheduled Create Flow

Scheduled booking bhi `LexhoExperienceController#createBooking` use karti hai.

Differences from instant:

1. Booking type `SCHEDULED`.
2. Backend `parseScheduledAt(date, slot)` call karta hai.
3. Scheduled validation:
   - date required
   - slot required
   - date format `YYYY-MM-DD`
   - selected date scheduled booking window ke andar
   - slot listed hai
   - slot expired nahi hai
   - slot available hai
4. `OrderService.placeOrder()` same cart-to-order flow run karta hai.
5. Order initially:
   - `status = CREATED`
   - `paymentStatus = PENDING`
   - `bookingType = SCHEDULED`
   - `scheduledAt = selected slot start`
6. `completeBookingDispatch` scheduled path me order ko `SCHEDULED` status me move kar sakta hai.
7. Dispatch immediately nahi hota agar scheduled pre-dispatch window abhi nahi aaya.

### 4.3 Scheduled Payment and Dispatch Flow

Payment same Razorpay flow use karta hai.

Payment captured:

1. `InstantBookingPaymentDispatchListener#onPaymentCaptured` run hota hai.
2. `dispatchPaidScheduledBooking(orderId)` call hota hai.
3. Backend checks:
   - booking type `SCHEDULED`
   - payment status `SUCCESS`
   - dispatch already started nahi
   - assigned partner nahi
   - scheduled time present
   - scheduled pre-dispatch window reached hai
4. Agar pre-dispatch window nahi aaya:
   - dispatch defer hota hai
   - booking scheduled state me rehti hai
5. Jab pre-dispatch window aata hai:
   - order `PARTNER_SEARCH` state me ja sakta hai
   - same dispatch candidate/skill/radius/ETA logic run hota hai
6. Scheduled dispatch max attempts:
   - `MAX_SCHEDULED_DISPATCH_ATTEMPTS = 5`
7. Agar partner nahi mila:
   - event `SCHEDULED_DISPATCH_RETRY_SCHEDULED`
   - later retry scan
8. Attempts exhaust ho jaaye:
   - unavailable reason `NO_PARTNER_AVAILABLE_MAX_ATTEMPTS`
   - event `SCHEDULED_DISPATCH_EXHAUSTED`

### 4.4 Scheduled Partner Lifecycle

Partner side lifecycle same hai:

1. Partner request/order visible hota hai.
2. Partner accepts.
3. Partner arrives.
4. Customer start PIN deta hai.
5. Partner starts service.
6. Completion PIN request/verify hota hai.
7. Partner completes service.
8. Order history/timeline update hoti hai.

## 5. Subscription Booking Flow

### 5.1 Customer App Flow

Entry files:

- `src/screens/cart/CartCrudScreen.tsx`
- `src/api/subscriptionBookingApi.ts`

Flow:

1. Customer subscription-capable service select karta hai.
2. App booking mode `SUBSCRIPTION` use karta hai.
3. Required data:
   - `serviceId`
   - `subscriptionServiceType`
   - `frequencyDays`
   - `startDate`
   - `endDate`
   - `visits`
   - `durationHours` for non-cook pricing
   - `familySize` for cook pricing
   - `timeSlot`
   - `addressId`
   - coordinates
   - optional coupon
4. Subscription backend currently legacy numeric address FK prefer karta hai.
5. Agar address id UUID hai, customer app `/api/address/list` se matching numeric address resolve karne ki try karta hai.
6. App payload banata hai:

```json
{
  "serviceId": 123,
  "bookingMode": "SUBSCRIPTION",
  "serviceType": "CLEANING",
  "frequencyDays": ["MONDAY", "WEDNESDAY"],
  "startDate": "YYYY-MM-DD",
  "endDate": "YYYY-MM-DD",
  "visits": 8,
  "durationHours": 2,
  "familySize": "SMALL",
  "timeSlot": "HH:mm-HH:mm",
  "addressId": 123,
  "latitude": 12.34,
  "longitude": 77.12,
  "couponCode": "OPTIONAL"
}
```

7. App endpoints try karta hai:
   - `POST /api/subscription/create`
   - fallback `POST /api/subscription/bookings`
8. Optional `Idempotency-Key` header supported hai.

### 5.2 Backend Subscription Create Flow

Backend controller:

- `SubscriptionController`

Endpoints:

- `POST /api/subscription/create`
- `POST /api/subscription/bookings`
- Same aliases under `/api/subscriptions/...` because controller request mapping includes both `/api/subscription` and `/api/subscriptions`.

Flow:

1. Controller subscription flags validate karta hai:
   - `subscriptionBookingEnabled`
   - `subscriptionCheckoutEnabled`
2. `SubscriptionBookingService.create(customerId, request, idempotencyKey)` call hota hai.
3. Service validates:
   - frequency days
   - date range
   - non-blank time slot
   - service exists
   - service supports subscription
   - address belongs to customer
   - geo fence / serviceability
4. Visit dates generate hote hain:
   - start date
   - end date
   - selected weekdays/frequency days
   - visits count
5. Pricing calculate hoti hai:
   - cook services me `familySize`
   - non-cook services me `durationHours`
6. Backend payment booking/order create karta hai:
   - `bookingType = SUBSCRIPTION`
   - `status = CONFIRMED`
   - `paymentStatus = PENDING`
   - `scheduledAt = startDate.atStartOfDay()`
7. `SubscriptionMaster` create hota hai:
   - `status = PENDING`
   - linked `bookingId`
   - customer, service, address, frequency, dates, visits, time slot, amount
8. `SubscriptionVisit` rows create hote hain:
   - each visit starts as `SCHEDULED`
9. Response me subscription id, booking id, pricing, plan fields, generated visits aate hain.

### 5.3 Subscription Payment Activation

Listener:

- `SubscriptionPaymentListener`

Flow:

1. Customer payment same payment order/verify system se karta hai.
2. Payment captured event publish hota hai.
3. `SubscriptionPaymentListener#onPaymentCaptured` run hota hai.
4. Listener call karta hai:
   - `subscriptionBookingService.activateByBookingId(bookingId)`
   - `subscriptionAutoDispatchService.dispatchForBooking(bookingId)`
5. Pending subscription `ACTIVE` hoti hai.
6. Due visits dispatch ke liye eligible ho jaate hain.

### 5.4 Subscription Auto Dispatch

Backend service:

- `SubscriptionAutoDispatchService`

Scheduler:

- `SubscriptionAutoDispatchScheduler`

Flow:

1. Scheduler due visits scan karta hai.
2. Scan interval property:
   - `${app.subscription.auto-dispatch.scan-ms:60000}`
3. Due visit ke liye service finds eligible partner.
4. Subscription dispatch skill check stricter hai:
   - exact `partner_service` sub-service mapping required
   - normal dispatch jaise legacy category fallback yahan use nahi hota
5. Preferred assigned partner hai to pehle use try hota hai.
6. Nahi to nearest eligible partners search hote hain.
7. Partner requests create hote hain:
   - `subscriptionId`
   - `subscriptionVisitId`
   - `cartId` initially visit/booking reference ho sakta hai
   - status `PENDING`
8. Partner app same `getNewDispatchRequests` me subscription requests bhi dekh sakta hai.

### 5.5 Subscription Partner Accept and Visit Order

Backend service:

- `PartnerRequestService#accept`
- `SubscriptionVisitOrderService`

Flow:

1. Partner subscription request accept karta hai.
2. App first normal partner request accept endpoint try karta hai:
   - `PATCH /api/v1/partner/requests/{requestId}/accept`
3. Backend request me `subscriptionId` and `subscriptionVisitId` dekhkar subscription path choose karta hai.
4. Partner eligibility validate hoti hai.
5. `SubscriptionVisitOrderService.accept(request, partnerId)` call hota hai.
6. Visit ke liye normal partner order create/link hota hai.
7. Request `cartId` visit order id se replace/update ho sakta hai.
8. Peer pending subscription requests reject hote hain.
9. `subscriptionAutoDispatchService.markPartnerAccepted(...)` assignment mark karta hai.
10. Us visit ka order normal partner lifecycle follow karta hai:
    - arrive
    - start PIN verify
    - complete PIN verify
    - complete
11. `SubscriptionVisitOrderService.syncStatus()` partner order status ko subscription visit status se sync karta hai.
12. Sab visits complete hone par subscription completed state me ja sakti hai.

### 5.6 Subscription Management APIs

Customer APIs:

- `GET /api/subscription/list`
- `GET /api/subscription`
- `GET /api/subscription/{subscriptionId}`
- `GET /api/subscription/{subscriptionId}/detail`
- `POST /api/subscription/{subscriptionId}/pause`
- `POST /api/subscription/{subscriptionId}/resume`
- `POST /api/subscription/{subscriptionId}/cancel`
- `GET /api/subscription/{subscriptionId}/calendar`
- `GET /api/subscription/{subscriptionId}/visits`
- `POST /api/subscription/{subscriptionId}/visits/{visitId}/skip`
- `POST /api/subscription/{subscriptionId}/visits/{visitId}/reschedule`

Allowed customer behavior:

- `PENDING`: cancel allowed.
- `ACTIVE`: pause/cancel allowed.
- `PAUSED`: resume/cancel allowed.
- `COMPLETED`: terminal.
- `CANCELLED`: terminal.

## 6. Booking History Flow

Customer app:

- `src/api/bookingApi.ts`
- `getBookingsHistoryPage`

API:

- `GET /api/bookings/history?page=0&size=12`

Backend response can return grouped sections:

- `instantBookings`
- `scheduledBookings`
- `subscriptions`

Customer app fallback grouping:

- `bookingType === INSTANT` -> instant tab
- `bookingType === SCHEDULED` -> scheduled tab
- `bookingType === SUBSCRIPTION` -> subscription tab
- `subscriptionId` present -> subscription tab

Important mapped fields:

- booking id / order id
- service id/name
- status
- assignment status
- payment status
- scheduled time
- partner summary
- subscription details
- tracking status
- status history
- retry payment flags
- customer actions like cancel/reschedule/call/track

## 7. Partner Shift, Skill, and Dispatch Prerequisites

Partner dispatch depends on these runtime prerequisites:

1. Partner account/profile must be eligible.
2. Partner must be online / in shift.
3. Partner shift must allow more active jobs.
4. Partner current location must exist.
5. Partner must be within radius/ETA limits.
6. Partner must have required skill mapping.
7. Partner must not be on leave/block/reject state.

Skill APIs in partner app:

- `GET /api/v1/partner/dispatch/skills`
- `PUT /api/v1/partner/dispatch/skills`

Backend skill storage:

- `partner_service` rows through `PartnerServiceSkillEntity`

Normal instant/scheduled dispatch accepts:

- legacy subcategory match
- legacy category match
- exact partner-service sub-service row
- category row match

Subscription dispatch accepts:

- exact partner-service sub-service row only

Known caveat:

- Skill is enforced when request is created.
- Partner accept path does not fully revalidate service skill again.
- If skill mapping changes after request creation but before accept, pending request may still be accepted.

## 8. Status Mapping Summary

Customer-facing statuses:

- `SEARCHING_PARTNER`: partner search chal raha hai.
- `SCHEDULED`: scheduled booking confirmed hai, dispatch window pending ho sakta hai.
- `PARTNER_ASSIGNED`: partner assigned/accepted.
- `PARTNER_ON_THE_WAY`: partner accepted/on way.
- `PARTNER_ARRIVED`: partner reached.
- `SERVICE_STARTED`: start PIN verified / service started.
- `SERVICE_IN_PROGRESS`: service running.
- `COMPLETED`: service complete.
- `INSTANT_FAILED`: instant partner nahi mila.
- `CANCELLED`: booking cancelled.

Partner lifecycle statuses:

- `CREATED`
- `ASSIGNED`
- `ACCEPTED`
- `ARRIVED`
- `IN_PROGRESS`
- `COMPLETED`
- `REJECTED`

Normal backend order statuses include:

- `CREATED`
- `CONFIRMED`
- `SCHEDULED`
- `SEARCHING_PARTNER`
- `PARTNER_SEARCH`
- `PARTNER_ASSIGNED`
- `PARTNER_ACCEPTED`
- `ARRIVED`
- `SERVICE_STARTED`
- `SERVICE_IN_PROGRESS`
- `SERVICE_COMPLETED`
- `PAYMENT_COMPLETED`
- `DELIVERED`
- `INSTANT_FAILED`
- `CANCELLED`

## 9. End-to-End Trace: Instant

```text
Customer CartCrudScreen
-> createBooking(INSTANT)
-> POST /api/v1/bookings/create
-> LexhoExperienceController#createBooking
-> validate address/geofence/serviceability
-> OrderService#placeOrder
-> Order CREATED + PENDING payment + cart clear
-> customer app locks cart and opens Payment
-> POST /api/payments/create-order with Idempotency-Key
-> Razorpay payment
-> POST /api/payments/verify
-> PaymentCapturedEvent
-> InstantBookingPaymentDispatchListener
-> DispatchOrchestrationService#dispatchPaidInstantBooking
-> SEARCHING_PARTNER
-> create PartnerServiceRequest PENDING
-> Partner app getNewDispatchRequests
-> PATCH /api/v1/partner/requests/{id}/accept
-> PartnerRequestService#accept
-> markPartnerAccepted
-> partner arrives
-> start PIN generated
-> partner verifies start PIN
-> IN_PROGRESS
-> complete PIN generated
-> partner verifies complete PIN
-> COMPLETED
-> payout/notifications/history/timeline
```

## 10. End-to-End Trace: Scheduled

```text
Customer CartCrudScreen
-> select date/slot
-> createBooking(SCHEDULED)
-> POST /api/v1/bookings/create
-> LexhoExperienceController#createBooking
-> parseScheduledAt(date, slot)
-> validate slot window/listed/not expired/available
-> OrderService#placeOrder
-> Order CREATED + PENDING payment + scheduledAt
-> completeBookingDispatch marks SCHEDULED
-> payment create/verify
-> PaymentCapturedEvent
-> dispatchPaidScheduledBooking
-> if pre-dispatch window not reached: defer
-> scheduler/listener retry near scheduledAt
-> PARTNER_SEARCH
-> PartnerServiceRequest PENDING
-> partner accept
-> arrive
-> start PIN verify
-> IN_PROGRESS
-> complete PIN verify
-> COMPLETED
```

## 11. End-to-End Trace: Subscription

```text
Customer CartCrudScreen
-> choose SUBSCRIPTION
-> createSubscriptionBooking()
-> POST /api/subscription/create
-> SubscriptionController#create
-> validate feature flags
-> SubscriptionBookingService#create
-> validate service/address/geofence/frequency/date/time/pricing
-> create payment Order with bookingType SUBSCRIPTION and payment PENDING
-> create SubscriptionMaster PENDING
-> create SubscriptionVisit rows SCHEDULED
-> customer pays bookingId
-> PaymentCapturedEvent
-> SubscriptionPaymentListener
-> activateByBookingId
-> dispatchForBooking
-> SubscriptionAutoDispatchService scans due visits
-> create subscription PartnerServiceRequest PENDING
-> partner accept
-> SubscriptionVisitOrderService creates/links normal visit order
-> visit order follows arrive/start/complete lifecycle
-> sync visit status
-> all visits complete -> subscription completed
```

## 12. Important Gaps / Notes

1. Normal instant/scheduled booking creation has frontend duplicate-submit guard, but booking create DTO does not carry a server idempotency key.
2. Payment order creation does use `Idempotency-Key`.
3. Subscription create accepts optional `Idempotency-Key`.
4. Dispatch is payment-gated for instant and scheduled bookings.
5. Scheduled dispatch only starts near the scheduled pre-dispatch window.
6. Partner request accept does not re-check skill after request creation.
7. Subscription dispatch skill matching is stricter than normal dispatch.
8. Partner app contains many fallback endpoints for compatibility, so canonical endpoint may not be the only endpoint hit in old deployments.
9. PIN lifecycle requires customer PIN verification for start and completion.
10. Partner order completion triggers payout ensure and active job decrement.

## 13. Source Files Used

Backend:

- `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`
- `application-module/src/main/java/com/lexho/user_service/service/OrderService.java`
- `application-module/src/main/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationService.java`
- `application-module/src/main/java/com/lexho/user_service/partnerrequest/service/PartnerRequestService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/controller/SubscriptionController.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionBookingService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionAutoDispatchService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionVisitOrderService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionPaymentListener.java`
- `application-module/src/main/java/com/lexho/user_service/payment/InstantBookingPaymentDispatchListener.java`
- `application-module/src/main/java/com/lexho/user_service/lexhopartner/controller/LexhoPartnerOrderController.java`
- `application-module/src/main/java/com/lexho/user_service/lexhopartner/service/PartnerOrderService.java`

Customer app:

- `src/screens/cart/CartCrudScreen.tsx`
- `src/api/bookingApi.ts`
- `src/api/paymentApi.ts`
- `src/api/subscriptionBookingApi.ts`
- `src/screens/booking/BookingHistoryScreen.tsx`
- `src/screens/payment/PaymentScreen.tsx`
- `src/screens/booking/BookingPaymentConfirmScreen.tsx`

Partner app:

- `src/services/partnerService.ts`
- `src/screens/main/NewOrdersScreen.tsx`
- `src/screens/main/ActiveJobScreen.tsx`
- `src/screens/main/OrderHistoryScreen.tsx`
- `src/hooks/useRealtimeDispatchRequests.ts`
- `src/features/shift/locationTrackingManager.ts`
