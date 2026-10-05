# Lifecycle Hardening Report

Date: 2026-10-01

## 1. Production Changes

- `order-service/src/main/java/com/lexho/booking/service/OrderLifecycleService.java`
  - Added shared authoritative lifecycle transition service.
  - Uses `findByIdForUpdate`, validates `canTransitionTo`, preserves same-status idempotency, and persists transitions.

- `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java`
  - `markPaid` now requests `CREATED -> CONFIRMED` through `OrderLifecycleService.transitionIfCurrent(...)`.
  - Preserves Razorpay/payment validation and payment persistence.
  - Prevents duplicate payment captured events for already-paid transactions.

- `payment-service/src/main/java/com/lexho/payment/service/AdminPaymentService.java`
  - `reconcileLatest` now requests `CREATED -> CONFIRMED` through `OrderLifecycleService.transitionIfCurrent(...)`.
  - Preserves reconciliation validation and payment persistence.
  - Prevents duplicate `PaymentCapturedEvent` on already-paid reconciliation.

- `application-module/src/main/java/com/lexho/user_service/booking/service/InstantBookingService.java`
  - Deprecated public `create(...)` now fails closed with `LEGACY_BOOKING_FLOW_DISABLED`.
  - This prevents accidental use of the legacy flow to create confirmed orders outside payment/dispatch lifecycle.

## 2. Test Changes

- `payment-service/src/test/java/com/lexho/payment/service/PaymentServiceTest.java`
  - Added coverage for cancelled/completed booking payment callbacks.
  - Verifies terminal orders do not become `CONFIRMED`.

- `payment-service/src/test/java/com/lexho/payment/service/AdminPaymentServiceTest.java`
  - Added duplicate reconciliation and terminal-state regression coverage.

- `payment-service/src/test/java/com/lexho/payment/service/PaymentServiceAdditionalCoverageTest.java`
  - Updated constructor wiring for `OrderLifecycleService`.

- `application-module/src/test/java/com/lexho/user_service/booking/service/InstantBookingServiceTest.java`
  - Updated tests to verify deprecated `create(...)` fails closed.

## 3. Payment Lifecycle

`PaymentService.markPaid` and `AdminPaymentService.reconcileLatest` no longer directly mutate existing order lifecycle status.

Both now use the authoritative lifecycle path:

```text
CREATED -> CONFIRMED
```

via `OrderLifecycleService.transitionIfCurrent(...)`.

Invalid/backward transitions remain blocked:

- `CANCELLED -> CONFIRMED`
- `COMPLETED/DELIVERED -> CONFIRMED`
- `FAILED -> CONFIRMED`
- other non-`CREATED` states

## 4. Legacy Instant Booking

`InstantBookingService.create` was kept but hardened.

It now immediately throws:

```text
LEGACY_BOOKING_FLOW_DISABLED
```

Repository search found no production callers. Matches found only in:

- `InstantBookingService`
- `InstantBookingServiceTest`

## 5. Direct Status Mutations

Final direct order-status mutation count: 15.

### Legitimate Initial State: 4

- `OrderService.placeOrder`: new order `CREATED`
- `SubscriptionBookingService`: subscription payment booking initial `CONFIRMED`
- `SubscriptionVisitOrderService`: per-visit partner order initial `ACCEPTED`
- `PartnerOrder`: default status initialization `CREATED`

### Authoritative Lifecycle Internals: 10

- `OrderLifecycleService.transition`
- `OrderService.updateStatus`
- `OrderService.cancelLockedOrder`
- `DispatchOrchestrationService` guarded dispatch transitions
- `LexhoExperienceController.applyOrderStatusTransition`
- `PartnerRequestService.applyOrderStatusTransition`
- `PartnerOrderService` partner-order lifecycle transition
- `InstantBookingService.cancel` guarded transition

### Guarded / Unreachable Legacy: 1

- Private `InstantBookingService.legacyCreate` still contains the old assignment, but public `create(...)` throws before it and there are no production callers.

### Unsafe Bypasses: 0

Payment-service direct order lifecycle status mutations: 0.

## 6. Scenario Verification

- unpaid booking cannot dispatch: PASS
- paid booking can dispatch: PASS
- duplicate dispatch idempotent: PASS
- duplicate partner acceptance idempotent: PASS
- repeated cancellation idempotent: PASS
- duplicate payment confirmation idempotent: PASS
- failed payment safe handling: PASS
- completed/cancelled orders cannot move backwards: PASS
- admin cancellation: PASS
- Instant flow: PASS
- Scheduled flow: PASS for lifecycle/dispatch tests
- Subscription flow: PASS
- subscription per-visit assignment/status: PASS

Note: `SlotBookingPaymentContractTest` has 2 unrelated scheduled metadata failures: expected `SCHEDULED_STATUS`, actual `INSTANT_STATUS`.

## 7. Build/Test Results

PASS:

```powershell
.\mvnw.cmd -DskipTests compile
```

PASS:

```powershell
.\mvnw.cmd -pl payment-service "-Dtest=PaymentServiceTest,PaymentServiceAdditionalCoverageTest,AdminPaymentServiceTest" test
```

Result: 34 tests, 0 failures.

PASS:

```powershell
.\mvnw.cmd -pl application-module "-Dtest=InstantBookingServiceTest" test
```

Result: 9 tests, 0 failures.

PASS:

```powershell
.\mvnw.cmd -pl application-module "-Dtest=OrderServiceTest,DispatchOrchestrationServiceTest,DispatchCleanupSchedulerTest,PartnerDispatchControllerTest,PartnerDispatchEligibilityServiceTest,TrackingLifecycleServiceTest,SubscriptionBookingLifecycleTest,SubscriptionBookingServiceTest,SubscriptionAutoDispatchServiceTest,AdminSubscriptionManagementServiceTest,SubscriptionControllersTest,InstantBookingServiceTest" test
```

Result: 102 tests, 0 failures.

PASS:

```powershell
.\mvnw.cmd -pl order-service,payment-service "-Dtest=BookingEntityLifecycleTest,OrderStatusConverterTest,OrderRepositorySearchOrdersTest,OrderRepositoryTest,BookingManagementServiceTest,PaymentServiceTest,PaymentServiceAdditionalCoverageTest,AdminPaymentServiceTest" test
```

Result:

- order-service: 35 tests, 0 failures, 2 skipped
- payment-service: 34 tests, 0 failures

PASS:

```powershell
.\mvnw.cmd "-Dmaven.test.skip=true" install
```

Known unrelated blocker:

- `-am test` remains blocked by `CallControllerMySqlContainerIT` missing Testcontainers imports.

Known unrelated observed failure:

- `SlotBookingPaymentContractTest`: 2 scheduled metadata assertions fail because `trackingScreen` returns `INSTANT_STATUS` instead of `SCHEDULED_STATUS`.

## 8. Remaining Risks

No unsafe lifecycle bypass remains from payment confirmation or legacy instant booking.

The only genuine remaining risk observed during verification is the unrelated scheduled booking metadata failure in `SlotBookingPaymentContractTest`.
