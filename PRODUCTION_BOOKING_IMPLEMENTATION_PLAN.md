# Production Booking Implementation Plan

**Audit mode:** Read-only source and contract audit. No source code was changed.  
**Audited implementation:** Current checked-out worktree, not an assumption that documentation or older artifacts are authoritative. The worktree already contains unrelated modifications; this report describes the source as found.

## 1. Executive Summary

The active backend is a single Spring Boot application assembled from Maven modules. The canonical one-time customer booking API is `LexhoExperienceController#createBooking`; instant dispatch is gated on captured payment and partner acceptance is protected by MySQL locks. The dedicated Razorpay service verifies signatures and persists webhook/idempotency state. Subscription visits have their own schedule, dispatch requests, assignment records, and order-backed completion path.

This is **not production-ready for all booking modes**. Scheduled bookings are persisted but no active scheduled-booking dispatch scan was found; the dispatcher's scheduled pre-assignment-window logic is therefore not reached by a scheduler. STOMP booking topics permit any authenticated customer/partner token to subscribe to a guessed booking id. A successful instant-payment event can be permanently lost between database commit and dispatch because booking dispatch is triggered by an in-process after-commit listener without a durable outbox/replay. Subscription admin assignment and replacement do not reliably change live dispatch/work assignments. The legacy customer refund route directly changes local payment status instead of initiating or verifying a Razorpay refund.

**Production gate:** Fix the booking-topic IDOR, scheduled dispatch, payment-to-dispatch recovery, subscription assignment integrity, and legacy payment/refund behavior before broad production rollout. Existing payment verification, webhook deduplication, customer ownership checks on canonical customer routes, partner acceptance locking, and order-backed PIN completion are useful controls, but they do not compensate for the gaps above.

## 2. Repository Map

| Area | Present in this repository | Current role / audit boundary |
|---|---|---|
| Java backend | Root Maven reactor: `common-lib`, `auth-service`, `admin-module`, `user-service`, `order-service`, `payment-service`, `partner-module`, `catalog-module`, `notification-module`, `application-module` | `application-module` is explicitly described as the single runtime application and depends on the other modules. It contains the active controllers, orchestration, subscription, dispatch, partner lifecycle, and realtime configuration. |
| Customer React Native app | No app source, React Native manifest, or customer navigation/screens found | Only API/OpenAPI and a notification JavaScript example exist under `docs/`; those are not evidence of a shipped RN client. |
| Partner React Native app | No app source or RN manifest found | `PARTNER_APP_README.md` and Postman collections document API surfaces but do not prove client behavior. |
| Admin React web app | No admin frontend package/source found | Admin APIs, Postman collection, and integration docs are present; no actual web client is auditable here. |
| Persistence | Flyway migrations under `application-module/src/main/resources/db/migration/application-module/`; MySQL is configured as the production database | `orders` is shared by customer/payment/order and partner lifecycle projections. Subscription visits and assignments add separate tables. |
| Redis | Dependency/configuration present in `application-module`; Redis-backed rate limiting, geo/index and supporting caches are visible | No Redis distributed lock for booking dispatch/acceptance was found. Do not treat Redis as booking-state authority. |
| Payment | `payment-service` module, included by `application-module` | Razorpay order creation, signature/webhook processing, payment/refund ledgers, webhook retries. A separate legacy payment surface remains in `LexhoExperienceController`. |
| Realtime | STOMP endpoints and a simple in-memory broker, plus SSE endpoints | Realtime transport exists in backend, but topic authorization, cluster delivery, and client integration are incomplete/unverifiable. |
| Tests and operations | Unit/controller tests, MySQL integration/readiness tests, Postman and PowerShell scripts | Coverage is stronger for service behavior and select MySQL lock/idempotency invariants than for complete end-to-end booking recovery across process failures. |

**Current implementation authority:** `application-module` sources and the service modules it includes are treated as current. Documentation under `docs/`, root readmes, generated `target/`, and Postman collections may be stale and must not override the implementation. No external app repositories or production database/Redis/Razorpay runtime were available in this checkout.

## 3. Instant Booking Audit

**Observed flow:** Customer `POST /api/bookings/create` or `/api/v1/bookings/create` -> `LexhoExperienceController#createBooking` validates address/serviceability/type, calls `OrderService#placeOrder`, persists dispatch metadata, then `completeBookingDispatch` calls `markInstantDispatchPending` (status moves to partner-searching; no partner request yet) -> customer creates Razorpay order via `POST /api/payments/create-order`, verifies checkout or webhook -> `PaymentService#markPaid` commits `PaymentStatus.SUCCESS` and emits `PaymentCapturedEvent` -> `InstantBookingPaymentDispatchListener#onPaymentCaptured` calls `DispatchOrchestrationService#dispatchPaidInstantBooking` -> candidate requests -> partner `PATCH /api/partner/requests/{requestId}/accept` -> order assignment -> partner arrives/starts/completes through `/api/orders/{id}/...` and customer PIN verification.

The payment gate is materially safer than dispatch-before-payment: `DispatchOrchestrationService#shouldWaitForPayment` blocks instant requests when payment is not `SUCCESS`, and partner acceptance rechecks the payment state. `dispatchPaidInstantBooking` locks the order and checks `dispatchStartedAt`/assignment before starting dispatch. The key remaining risk is recovery after payment commit, not the intended ordering.

### Finding IB-1 - Payment capture can commit without durable dispatch

**Severity:** High  
**Application:** Java Spring Boot backend  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/payment/InstantBookingPaymentDispatchListener.java`, `InstantBookingPaymentDispatchListener#onPaymentCaptured`; `application-module/src/main/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationService.java`, `DispatchOrchestrationService#dispatchPaidInstantBooking`; `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java`, `PaymentService#markPaid`  
**Current behavior:** Payment status and webhook idempotency are committed before the after-commit listener creates partner requests. The `dispatchStartedAt` guard prevents a repeat after a successful dispatch, but no durable dispatch command/outbox or recovery scan replays a capture whose listener fails or whose process stops after commit. A duplicate capture is treated as already paid and does not emit a new `PaymentCapturedEvent`.  
**Expected production behavior:** Every committed paid instant booking must eventually be dispatched exactly once logically, or enter a durable visible failure/retry state.  
**Root cause:** `@TransactionalEventListener(AFTER_COMMIT)` is an in-process callback, not a durable delivery mechanism; the payment webhook retry may not repeat business dispatch after payment has already been recorded as paid.  
**Recommended fix:** Write a dispatch-outbox record in the same payment transaction; consume with retries and a booking-level idempotency key/claim, record terminal failure and alerting. Reconciliation should find paid instant orders with no accepted/active dispatch and replay safely.  
**Required tests:** Commit captured payment then force listener/process failure; prove a later worker replays. Run duplicate capture/retry workers concurrently and assert one logical active dispatch and no skipped paid booking.

## 4. Scheduled Booking Audit

`createBooking` sets the order type and `scheduledAt`, then `markScheduledBooking` transitions `CREATED` to `SCHEDULED`. Dispatch contains a useful `shouldDeferDispatch` check based on a pre-dispatch window, but the active controller does not call scheduled dispatch. Search of active sources found `dispatchOrder` only in a private helper that is not called by `completeBookingDispatch`; no scheduler scans scheduled one-time orders. `DispatchCleanupScheduler` cleans stale shift/request state and is not a scheduled-booking dispatcher.

### Finding SB-1 - Scheduled bookings are stored but never dispatched

**Severity:** High / production blocker for scheduled bookings  
**Application:** Java Spring Boot backend  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`, `createBooking`, `completeBookingDispatch`, `dispatchInstantBooking`, `markScheduledBooking`; `application-module/src/main/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationService.java`, `shouldDeferDispatch`; `application-module/src/main/java/com/lexho/user_service/dispatch/scheduler/DispatchCleanupScheduler.java`, `cleanupStaleDispatchState`  
**Current behavior:** Scheduled orders are marked `SCHEDULED` and returned to customers without partner requests. The scheduled dispatch deferral logic exists but is not invoked by an active scheduled-order scan.  
**Expected production behavior:** A scheduled order becomes eligible for partner dispatch once its configured pre-dispatch window opens; dispatch retries and no-partner outcomes are observable and recoverable.  
**Root cause:** Creation marks the order only; there is no active one-time scheduled booking scheduler/query/claim path wired to `DispatchOrchestrationService`.  
**Recommended fix:** Add a bounded, indexed, database-claimed scan of eligible scheduled orders and dispatch only within the configured lead window. Add idempotent attempt/terminal states and operational retry.  
**Required tests:** Assert no request before the window; exactly one dispatch within it; correct behavior for multiple scheduler instances, late scans, cancellation, reschedule, missing address/service, and zero eligible partners.

### Finding SB-2 - Reschedule contract is not canonical

**Severity:** Medium  
**Application:** Backend/API contract  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`, `rescheduleBooking`, `rescheduleBookingV2`; `application-module/src/main/java/com/lexho/user_service/lexho/dto/LexhoDtos.java`, `RescheduleRequest` and `RescheduleSlotRequest`; `docs/booking-flow-openapi.yaml`, `/api/bookings/{bookingId}/reschedule`  
**Current behavior:** There are two incompatible routes: legacy `PUT /api/bookings/reschedule/{id}` accepts only `date`; `POST /api/bookings/{id}/reschedule` accepts `date` and `slot`. The OpenAPI route is unversioned and does not define a request body; neither implementation provides a `/api/v1/bookings/{id}/reschedule` alias. The older `BookingRescheduleRequest`/`InstantBookingService#reschedule` shape is another unused contract surface.  
**Expected production behavior:** Customer clients use one versioned reschedule route, documented request/response, timezone semantics, eligibility, slot validation, and a consistent conflict/error response.  
**Root cause:** Legacy and new API implementations coexist; contract generation/docs do not express payload requirements or route versioning.  
**Recommended fix:** Select a canonical versioned path and date/slot DTO, validate it against availability and status, maintain only a deliberate compatibility alias during migration, then align OpenAPI/Postman/client contracts.  
**Required tests:** Contract tests for both supported versions, invalid/expired slot, already assigned/in-progress/cancelled booking, timezone boundaries, and reschedule count limits.

### Finding SB-3 - Reschedule can leave stale dispatch state

**Severity:** Medium  
**Application:** Java Spring Boot backend  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`, `LexhoExperienceController#rescheduleBooking`, `LexhoExperienceController#rescheduleBookingV2`; `application-module/src/main/java/com/lexho/user_service/partnerrequest/service/PartnerRequestService.java`, `PartnerRequestService#accept`  
**Current behavior:** Legacy reschedule changes `scheduledAt` after checking ownership but does not validate that the order is reschedulable, nor invalidate or replace existing partner requests. V2 does check `canReschedule`, but updates the order without synchronizing requests/attempt state.  
**Expected production behavior:** Rescheduling atomically supersedes old partner offers and prevents acceptance against the old slot.  
**Root cause:** Order schedule and partner requests are managed in separate records without a reschedule transition/reconciliation.  
**Recommended fix:** Lock order, validate status and request limits, supersede pending offers, update dispatch generation/version, then schedule a new dispatch attempt.  
**Required tests:** Race acceptance against reschedule; ensure the old request cannot win and canceled/accepted/in-progress orders cannot be rescheduled.

## 5. Subscription Booking Audit

**Observed flow:** `SubscriptionController#create` -> `SubscriptionBookingService#create` validates catalog/address/pricing, creates a pending payment `orders` row, `subscription_master`, frequencies and scheduled visits -> Razorpay capture -> `SubscriptionPaymentListener#onPaymentCaptured` activates and calls `SubscriptionAutoDispatchService#dispatchForBooking` -> per-visit partner requests -> partner acceptance calls `SubscriptionVisitOrderService#accept`, which locks the visit, creates a `PartnerOrder`/shared `orders` record, links `subscription_visit.booking_id`, and marks visit accepted -> partner lifecycle updates visit status -> last visit completion marks subscription completed. `SubscriptionAutoDispatchScheduler` also scans due visits for active subscriptions.

Pause/resume/cancel and customer reschedule/skip APIs exist. Customer ownership is checked by `getOwnedSubscription`. There are distinct subscription-level preferred partner assignment and per-visit assignment records; the distinction is not consistently wired to dispatch/work.

### Finding SUB-1 - First visit is dispatched immediately, irrespective of visit date

**Severity:** High  
**Application:** Java Spring Boot backend  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionPaymentListener.java`, `onPaymentCaptured`; `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionAutoDispatchService.java`, `dispatchForBooking`, `autoAssignVisit`, `autoAssignDueVisits`  
**Current behavior:** Payment capture immediately chooses the first scheduled/rescheduled visit and dispatches it. This path bypasses `isDueForDispatch`, which is used by the periodic scheduler. A subscription whose first visit is weeks away can send a short-expiry request immediately.  
**Expected production behavior:** Only dispatch a visit inside its configured pre-assignment window unless the product explicitly promises advance partner assignment with durable assignment and no short request expiry.  
**Root cause:** Immediate payment listener calls an un-gated path and conflates subscription activation with per-visit dispatch timing.  
**Recommended fix:** Apply the same visit due-window eligibility to immediate and scheduled paths; if advance partner selection is intentional, model it as a durable subscription-level assignment rather than an expiring visit offer.  
**Required tests:** Capture subscriptions with first visits inside/outside the window; verify request timing, expiry/retry, and subsequent visit order.

### Finding SUB-2 - Per-visit dispatch is not atomically claimed

**Severity:** High  
**Application:** Java Spring Boot backend / MySQL  
**File / Class / Method:** `SubscriptionAutoDispatchService#autoAssignDueVisits`, `autoAssignVisit`, `createDispatchRequests`; `subscription/repository/SubscriptionVisitRepository.java`; `partnerrequest/repository/PartnerServiceRequestRepository.java`  
**Current behavior:** Scheduler and payment/manual retry paths check for an assignment and active request, then insert a request. The visit is not locked/claimed while making this decision and there is no uniqueness constraint for one active pending request per visit/attempt.  
**Expected production behavior:** Parallel schedulers, event retries, and manual retries must not create duplicate dispatch offers or duplicate assignment work.  
**Root cause:** Check-then-insert is transactional but not serialized by a common visit lock/claim; uniqueness currently protects accepted assignment records, not active requests.  
**Recommended fix:** Claim/lock the visit before querying/inserting or use a unique dispatch-generation/active-claim record; make retries idempotent across application replicas.  
**Required tests:** Concurrent payment listener + scheduler + retry for one visit on MySQL; assert one active request per partner/visit and one stable attempt generation.

### Finding SUB-3 - Admin per-visit assignment can suppress dispatch without creating work

**Severity:** High  
**Application:** Admin backend / subscription dispatch  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/subscription/service/AdminSubscriptionManagementService.java`, `assignVisitPartner`; `SubscriptionAutoDispatchService#autoAssignDueVisits`; `SubscriptionVisitOrderService#accept`  
**Current behavior:** Admin assignment writes `subscription_visit_partner_assignment` only. The auto-dispatch scan skips any visit with that record, but the operation does not create a partner request, `PartnerOrder`, or `subscription_visit.booking_id`. The partner therefore may receive no actionable work while automatic dispatch is suppressed.  
**Expected production behavior:** Admin assignment must either create a complete dispatch/work assignment or explicitly mean only a preferred partner while normal dispatch continues.  
**Root cause:** The admin assignment record is treated as a completed assignment by the scheduler, but is not connected to the operational request/order lifecycle.  
**Recommended fix:** Implement admin assignment as a transactionally consistent dispatch command (validate partner, lock visit, create request/order and link assignment), or separate preference from confirmed assignment and do not suppress dispatch.  
**Required tests:** Admin assign then partner sees/accepts the correct visit, one order is created, visit completion advances subscription; failed assignment rolls back without blocking fallback.

### Finding SUB-4 - Replacement does not replace live/future visit work

**Severity:** High  
**Application:** Admin backend / subscription dispatch  
**File / Class / Method:** `AdminSubscriptionManagementService#replacePartner`, `assignPartner`, `assignVisitPartner`; `SubscriptionAutoDispatchService#preferredPartnerId`, `createDispatchRequests`  
**Current behavior:** Replacement changes fields on `subscription_partner_assignment` (`replacementPartnerId`, status, reason, timestamps). It does not revoke pending offers, reassign existing `subscription_visit_partner_assignment` rows, replace `PartnerOrder.assignedPartnerId`, or dispatch work for affected visits. Future partner requests already generated against the old partner remain actionable until expiry.  
**Expected production behavior:** Replacement has explicit scope (future unassigned visits vs active visit), supersedes old offers, preserves completed history, and routes each eligible visit to the replacement or fallback.  
**Root cause:** Replacement is represented as metadata/preference; no transactional reconciliation of visit requests and work orders is performed.  
**Recommended fix:** Define replacement effective date/visit scope; lock subscription and affected visits, supersede old requests, update only eligible unstarted work, create replacement dispatch, and audit each change.  
**Required tests:** Replace before dispatch, with pending request, after partner acceptance, mid-service, and after completed visits; assert no stale partner can accept/continue a superseded visit.

### Finding SUB-5 - Pause/cancel do not invalidate all visit offers

**Severity:** Medium  
**Application:** Java Spring Boot backend  
**File / Class / Method:** `SubscriptionBookingService#pause`, `resume`, `cancel`; `SubscriptionVisitRepository#cancelFutureScheduled`; `PartnerRequestService#accept`  
**Current behavior:** Cancellation bulk-updates only future visits whose status is `SCHEDULED`, not `RESCHEDULED`, and pause/cancel do not expire their pending partner requests. Acceptance later checks the subscription is active for the subscription-order path, so stale offers can remain visible and fail on accept rather than being withdrawn.  
**Expected production behavior:** Pause/cancel atomically prevent new dispatch and withdraw all future scheduled/rescheduled offers; already accepted/in-progress visit treatment is explicit.  
**Root cause:** State changes update master/visit records but do not reconcile request rows and the bulk query omits `RESCHEDULED`.  
**Recommended fix:** Lock subscription and visits, transition all eligible future states, expire associated pending requests, and define treatment for accepted/in-progress visits.  
**Required tests:** Pause/cancel while a request is pending, while a visit is rescheduled, and during acceptance race; verify partner list and customer calendar agree.

### Finding SUB-6 - Subscription creation has no request-level idempotency

**Severity:** Medium  
**Application:** Customer booking API  
**File / Class / Method:** `SubscriptionController#create`; `SubscriptionBookingService#create`  
**Current behavior:** Each valid create call builds a new payment booking, subscription, frequencies and visits; no idempotency key or duplicate request fingerprint is used.  
**Expected production behavior:** Retries after client timeout must return the original subscription/payment booking rather than create duplicate plans and charges.  
**Root cause:** Creation relies on client behavior/rate limiting and payment-order idempotency begins only after a booking exists.  
**Recommended fix:** Add customer-scoped create idempotency persisted under a unique key and bind it to normalized request data.  
**Required tests:** Repeat same key concurrently, retry after timeout, and reject key reuse with materially different schedule/pricing input.

## 6. Customer App Audit

No Customer React Native application source exists in this repository. The backend API and sample JS under `docs/customer-websocket/` are not the customer app. The creation contract is partly versioned; other booking details/history/cancel/reschedule/payment/subscription endpoints mix versioned and unversioned paths. Customer-facing `meta` and `trackingRefreshSeconds` are backend fields, but no actual client handling, offline behavior, payment callback, retry UX, or polling backoff can be verified.

Customer ownership checks are present in canonical instant detail/cancel/reschedule and subscription reads/mutations. Legacy customer cancellation (`OrderService#cancelOrder`) checks ownership, but stale partner requests are not synchronously withdrawn. The customer refund route is addressed under Payment Audit.

**Required client follow-up:** Audit the actual released RN commit separately; align endpoint/base URL, payload, timezone, auth refresh, polling/realtime fallback, payment verification and stale request handling against the canonical contracts before rollout.

## 7. Partner App Audit

No Partner React Native source exists. Server-side partner request list/detail/accept/reject APIs are present under `/api/partner` and `/api/v1/partner`; order lifecycle APIs are under `/api/orders` and `/api/v1/orders`. Partner identity comes from the JWT-backed `userId` request attribute. Request acceptance validates expiry, eligibility, booking status and instant payment; service start/complete require customer PIN verification. The actual partner UI's handling of accepted vs assigned statuses, expiry, retries, offline mode, and subscription visits cannot be audited here.

Parallel legacy surfaces exist: `PartnerRequestService` handles dispatch request acceptance while `PartnerOrderService` handles order lifecycle. Verify actual partner client calls the request-based accept API for dispatch and only uses order lifecycle after assignment; do not assume README examples reflect released client behavior.

## 8. Admin Audit

Admin controllers generally use `@PreAuthorize("hasRole('ADMIN')")`; subscription admin routes provide status changes, partner assignment/replacement and per-visit assignment. Normal booking-management routes support listing/details/status/cancel/tracking/payment reconciliation but no general explicit admin-to-partner assignment route was found. Admin status changes can alter operational order state but are not a substitute for assignment records/request/order creation.

Subscription admin `updateStatus` permits `PENDING -> ACTIVE`; this path does not check the linked booking's payment state, while the subscription scheduler dispatches active subscriptions. Restrict activation to verified payment or record an audited override with explicit exception handling and block dispatch until approved. Admin frontend source is absent, so UI guards cannot be verified.

## 9. Payment Audit

**Canonical path:** `PaymentController#createOrder` requires an idempotency header; `PaymentService#createOrder` reads amount from the owned booking, calls Razorpay, stores a `PaymentTransaction`, then checkout `verify` verifies signature and fetches payment details/amount. Webhooks verify Razorpay signature, persist an idempotency-keyed event, lock transactions, and schedule retries for handler exceptions. `PaymentCapturedEvent` updates booking payment status. Admin refund code uses a payment/refund ledger.

### Finding PAY-1 - Legacy customer refund fabricates local payment failure

**Severity:** Medium  
**Application:** Customer/payment backend  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java`, `refund`  
**Current behavior:** The customer endpoint checks booking ownership but directly sets the order payment status to `FAILED` and returns “Refund requested”; it does not create a Razorpay refund, check the refund ledger, or verify gateway settlement.  
**Expected production behavior:** A refund request must create a durable, authorized refund workflow; booking/payment state changes only after verified Razorpay outcome.  
**Root cause:** A legacy compatibility endpoint mutates local state outside `PaymentService` and the refund ledger.  
**Recommended fix:** Remove/disable the state-mutating customer route or route it through an explicit refund request workflow. Only gateway-signed/refund-service reconciliation should mark a refund processed.  
**Required tests:** Customer cannot directly set payment state; refund request idempotency; gateway success/failure/partial refund webhook reconciliation.

### Finding PAY-2 - New idempotency key can create another Razorpay order after payment

**Severity:** High  
**Application:** Payment backend  
**File / Class / Method:** `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java`, `createOrder`, `retryOrder`  
**Current behavior:** Existing `Idempotency-Key` is deduplicated, but `createOrder` does not reject a booking already `SUCCESS`/`REFUNDED` and does not return an existing active attempt for a new key. A paid booking can therefore receive a second provider order under a different key.  
**Expected production behavior:** A booking has one payable amount/active attempt policy; already-paid or fully refunded bookings cannot silently create another charge attempt.  
**Root cause:** Idempotency is request-key scoped, not enforced against booking payment state/active transactions.  
**Recommended fix:** Lock booking and enforce payment-state and active-attempt rules; add unique/serialized booking attempt handling and explicit retry transitions.  
**Required tests:** New key after paid, concurrent distinct keys on one booking, retry after failed/expired attempt, and duplicate webhook/verify across attempts.

### Finding PAY-3 - Capture webhook does not reconcile captured amount/currency

**Severity:** Medium  
**Application:** Payment backend  
**File / Class / Method:** `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java`, `handleCaptured`, `markPaid`  
**Current behavior:** Checkout `verify` checks gateway order and amount, but signed `payment.captured` webhooks identify the stored Razorpay order and then mark the transaction/booking paid without comparing webhook amount/currency with the persisted transaction.  
**Expected production behavior:** Every payment channel must validate provider order, captured amount, and currency before marking a booking fully paid.  
**Root cause:** Validation is applied on checkout verification but not on the webhook capture path.  
**Recommended fix:** Parse and validate authenticated webhook fields against the stored transaction and the provider order before `markPaid`; route partial captures to an explicit state/reconciliation path.  
**Required tests:** Correct capture, mismatched amount/currency, partial capture, unknown order/payment, and duplicate capture.

**Payment-safe controls:** Webhook signature and event idempotency, row locks for provider order transaction, order ownership on payment APIs, server-derived booking amount, checkout signature verification, webhook retry/dead-letter ledger, unique transaction idempotency key/order/payment ids, and separate admin reconciliation/refund services exist. The webhook retry scheduler can recover webhook-processing failures; it does not by itself guarantee post-commit booking dispatch (IB-1).

## 10. Dispatch Audit

Partner matching uses serviceability, geo candidates, skill filters, shift/partner eligibility and scoring; dispatch revalidates candidate eligibility while locking partner shift rows. Instant requests are created only after captured payment. Up to five partner requests are created per dispatch wave; request expiry/reject paths can continue dispatch and radius expansion.

The normal one-time booking and subscription visit dispatch paths are different. One-time instant booking uses `orders.dispatch_*` metadata and a per-order `dispatchStartedAt`; subscription uses visit-specific request rows and assignment records. Their retry/assignment integrity should not be merged implicitly. Scheduled one-time orders currently lack a wired dispatch worker (SB-1). Subscription assignment/replacement and duplicate-dispatch issues are SUB-2 through SUB-4.

No booking-level Redis lock was found. Existing Redis geo/cache/rate-limit operations are optimizations, not authority for assignment. Persist state/claims in MySQL and retain transactional locks/unique constraints.

## 11. Scheduler Audit

| Job | Current behavior | Production concern |
|---|---|---|
| `InstantBookingSearchTimeoutScheduler#expireStuckSearches` | Scans instant partner searches past configured timeout and expires stuck state | Verify that timeout result is paired with a retry/fallback and payment/refund policy; no durable capture-dispatch replay. |
| `SubscriptionAutoDispatchScheduler#autoAssignDueVisits` | Every configured delay (default 60s) scans active subscriptions and scheduled/rescheduled visits | Unbounded active-subscription scan and no atomic claim across replicas; does not dispatch until due, but payment listener bypasses due gate. |
| `PartnerRequestExpiryScheduler#expirePendingRequests` | Expires stale pending partner offers | Must reconcile order/visit assignment and ensure repeated expiry/retry does not duplicate waves. |
| `DispatchCleanupScheduler#cleanupStaleDispatchState` | Periodic stale dispatch/shift cleanup | Does not dispatch scheduled one-time orders. |
| `PaymentWebhookRetryScheduler` | Retries failed/pending persisted webhook events | Good for webhook handler failures, not a replacement for an outbox for post-commit business side effects. |

All scheduled jobs use application-level schedules with no common distributed scheduler lock/claim observed. `@Transactional` alone does not prevent two service replicas from selecting the same due records.

## 12. Concurrency Audit

**Positive controls:** `PartnerServiceRequestRepository#findByIdAndPartnerIdForUpdate` locks a request. Acceptance then locks the normal order with `OrderRepository#findByIdForUpdate`; `DispatchOrchestrationService#markPartnerAccepted` again locks the order and rejects a different assigned partner. `V154__instant_booking_payment_dispatch_safety.sql` adds generated unique `accepted_cart_id` so one non-subscription order cannot have two `ACCEPTED` request rows. `ApplicationMysqlConcurrencyIT#concurrentPartnerAcceptanceUsesMysqlRowLockAndAllowsOnlyOneAssignment` exercises MySQL acceptance contention.

**Residual gaps:** Lock order is not unified/documented across every acceptance and admin mutation. Subscription due dispatch uses check-then-insert without a visit claim or active-request unique constraint. Admin visit assignment and scheduler race over assignment presence. Instant `dispatchStartedAt` handles concurrent invocation only once the order lock is held, but post-commit recovery is absent. No `@Version` optimistic version is used on key booking entities.

Partner acceptance is **partially production-safe**, not fully proven: validate conflict/deadlock behavior and committed state for expiry/error branches, since runtime exceptions after mutating request state may roll those mutations back.

## 13. API Contract Audit

| Operation | Backend contract found | Assessment |
|---|---|---|
| One-time create | `POST /api/bookings/create`, `POST /api/v1/bookings/create`; default booking type is `SCHEDULED` | Two aliases; confirm clients always send booking type intentionally. |
| Payment order/verify | `POST /api/payments/create-order`, `/verify`, `/retry`; `GET /api/payments/{bookingId}` | Explicit service contract, with idempotency on create-order; preserve only this as production checkout path. |
| Legacy payment | `/api/payments/initiate`, `/status/{bookingId}`, `/refund`, `/legacy-options` | Coexists with canonical Razorpay API; initiate only marks local payment pending and refund mutates local status. Retire or clearly isolate from live payments. |
| Scheduled reschedule | `PUT /api/bookings/reschedule/{id}` date-only and `POST /api/bookings/{id}/reschedule` date+slot | Incompatible payload/methods; no versioned reschedule alias, incomplete OpenAPI request schema (SB-2). |
| Subscription create/manage | `/api/subscription/**` and `/api/subscriptions/**`; creation aliases `/create` and `/bookings`; status/visits/calendar/pause/resume/cancel/skip/reschedule | Broad compatibility surface; client contract must settle on a canonical prefix and exact response schema. |
| Partner requests/orders | `/api/partner/requests/**` and `/api/orders/**` | Separate assignment and work-lifecycle APIs; client integration needs release-source verification. |
| Admin | `/api/v1/admin/booking-management/**`, `/api/admin/subscriptions/**` | Admin subscription assignment exists; general non-subscription booking assignment was not found. |

`docs/booking-flow-openapi.yaml`, Postman, README contracts, and actual controller mappings are not fully synchronized. Make generated contract tests part of release gating and use stable machine-readable error codes/statuses instead of relying on success-shaped “cannot reschedule” responses.

## 14. Realtime Audit

Backend STOMP connection endpoints include `/ws-notifications`, `/ws/customer/bookings`, `/ws/customer/bookings/{bookingId}`, `/ws/admin/bookings/{bookingId}`, and `/ws-chat`; simple broker topics include customer notification, booking lifecycle, ETA, chat, and admin notifications. `BookingLifecycleEventService` stores a `booking_events` record then sends `/topic/customer/bookings/{bookingId}`. ETA pushes use `/topic/customer/bookings/{bookingId}/eta`. Notification sample subscribes to `/topic/notifications/{customerId}`.

### Finding RT-1 - Booking and notification STOMP topics lack per-user authorization

**Severity:** High  
**Application:** Realtime/backend security  
**File / Class / Method:** `application-module/src/main/java/com/lexho/user_service/config/SecurityConfig.java`, `filterChain`; `application-module/src/main/java/com/lexho/user_service/notification/config/NotificationWebSocketConfig.java`, `registerStompEndpoints`, `configureMessageBroker`; `application-module/src/main/java/com/lexho/user_service/chat/config/ChatStompAuthChannelInterceptor.java`, `preSend`, `authorizeSubscription`; `application-module/src/main/java/com/lexho/user_service/booking/service/BookingLifecycleEventService.java`, `publish`; `application-module/src/main/java/com/lexho/user_service/map/service/NavigationEtaPushService.java`, `publish`; `application-module/src/main/java/com/lexho/user_service/notification/event/NotificationDispatchListener.java`  
**Current behavior:** STOMP CONNECT requires a valid JWT via the inbound interceptor, but the interceptor only authorizes chat-room destinations. Any authenticated customer or partner can subscribe to a guessed `/topic/customer/bookings/{bookingId}` or `/topic/notifications/{customerId}`. The booking payload reveals payment/status/assigned partner and ETA; notifications are broadcast to an id-addressed topic. Admin booking destinations are registered but lack a corresponding booking-level subscription authorization path.  
**Expected production behavior:** Authenticate and authorize every booking/notification subscription against principal ownership, assigned partner or explicit admin role; never authorize based only on a guessed numeric id.  
**Root cause:** Security permits the websocket handshake and the STOMP interceptor restricts only chat rooms, while event delivery uses public topic destinations.  
**Recommended fix:** Use authenticated user destinations or add strict destination authorization querying booking/customer/partner access; enforce role and booking ownership on SUBSCRIBE for every private topic and test admin role separation.  
**Required tests:** Valid customer subscribes to own booking; different customer/partner is rejected; assigned partner/admin allowed only by explicit policy; arbitrary customer/notification IDs denied; subscription cannot bypass via endpoint alias.

**Transport caveats:** The configured simple broker is process-local; it does not provide multi-replica fan-out without an external broker/relay. `BookingLifecycleEventService` sends before the surrounding transaction is guaranteed committed, so a client can see a state that rolls back. No production customer/partner/admin app source exists to verify endpoint, topic, JWT handshake header, reconnect, ordering, or polling fallback consistency. SSE customer/partner endpoints exist, but no booking polling client implementation or ETag/updated-since contract was found.

## 15. Database Audit

**Good constraints:** `orders` has dispatch metadata and timeout index from V154; V154's generated unique accepted-cart index prevents two accepted normal requests per booking. `subscription_visit.booking_id` has a unique index and FK to `orders` (V139); subscription-level assignment is unique per subscription and per-visit assignment unique per visit (V65). Payment transaction idempotency key, Razorpay order id, and payment id are unique; webhook event id/key and refund idempotency have unique constraints. These protections should be confirmed in deployment schema, not inferred solely from entity annotations.

**Gaps:** No equivalent unique/atomic constraint guarantees one active pending dispatch per booking/visit. Subscription frequency/day combinations and scheduled visits do not show a uniqueness guard for duplicate weekday/date generation. Key booking/subscription tables lack optimistic version columns. Cross-module order projections share tables and use native SQL (`SubscriptionVisitOrderService#accept`), raising schema/lifecycle coupling. `booking_events` persists audit sequence but no transactional outbox delivery status. There is no DB FK shown for all scalar `partner_service_requests` references; verify complete migrated schema and orphan cleanup.

## 16. Security Audit

1. **High - STOMP booking-topic IDOR (RT-1).** Authenticated customer/partner can observe arbitrary booking/ETA topics; per-booking authorization is missing.
2. **Medium - Customer refund endpoint can forge local payment state (PAY-1).** Ownership does not make a direct client-controlled `FAILED` mutation a real refund.
3. **Authorization strengths:** Admin management controllers use role annotations, customer booking/subscription reads and writes generally scope by authenticated user, partner dispatch acceptance scopes request by `partnerId`, webhook is signature-verified, and payment reads/verify check ownership.
4. **Additional hardening:** Ensure admin STOMP access has actual ADMIN role support rather than the chat interceptor's customer/partner-only principal model; validate scheduled/booking DTO size/ranges and rate limits consistently; do not trust legacy `amount`, `bookingId`, or local status inputs when a canonical payment service exists.

This is a booking-focused security assessment, not a full SAST, dependency, infrastructure, or deployed configuration penetration test.

## 17. Test Coverage Audit

**Existing relevant tests found:** `InstantBookingServiceTest`; dispatch orchestration, skill/eligibility/scoring, cleanup and partner dispatch controller tests; `PartnerRequestService` and partner-order service tests; subscription booking, lifecycle, auto-dispatch, pricing, admin, controller and repository tests; payment service/controller/admin tests; `SlotBookingPaymentContractTest`; MySQL concurrency, migration, upgrade and readiness ITs; Postman collections and operational smoke/regression scripts.

**Coverage gaps against production outcomes:**

- No test proves a scheduled one-time booking is dispatched at the correct pre-window (the active scheduler path is absent).
- No fault-injection test proves a committed instant payment is recovered if its after-commit dispatch listener fails.
- Current concurrency IT has normal partner acceptance coverage, but no subscription scheduler multi-worker dispatch race test.
- No end-to-end test covers subscription create -> capture -> visit request -> accept -> order lifecycle -> complete -> subsequent due visit, including payout and assignment replacement.
- No test covers admin visit assignment resulting in partner-visible work or replacement invalidating old pending requests.
- No test asserts webhook amount/currency mismatch is not accepted; no test blocks new Razorpay attempt for a paid booking.
- No STOMP tests reject another customer's booking topic or guessed notification topic.
- API contract tests do not prove all route aliases/payload schemas match OpenAPI and client Postman examples.
- No actual customer RN, partner RN, or admin web app tests are in this repository.

## 18. Prioritized Fix Plan

| Priority | Work | Why it gates production |
|---|---|---|
| P0 | Authorize booking/notification STOMP subscriptions per principal and stop exposing arbitrary booking topics | Customer/partner booking and ETA data can be read cross-account (RT-1). |
| P0 | Wire and test scheduled one-time dispatch with window gating and atomic claims | Scheduled orders currently have no active dispatch path (SB-1). |
| P0 | Add durable captured-payment -> dispatch outbox/reconciliation and idempotent consumer | Paid instant jobs can remain undispatched indefinitely after post-commit failure (IB-1). |
| P1 | Fix subscription assignment, replacement, pending offer cleanup, and per-visit claim semantics | Admin assignment can suppress dispatch; replacements/stale offers and concurrent schedulers break visit integrity (SUB-2 through SUB-5). |
| P1 | Remove/retire legacy local payment/refund endpoints; enforce paid/active-attempt rules and webhook amount/currency | Avoid forged refund state, duplicate charge attempts, and accepting mismatched capture (PAY-1 through PAY-3). |
| P1 | Canonicalize/version reschedule and enforce atomic cancellation/reschedule with dispatch state | Prevent stale offer acceptance and client/API divergence (SB-2/SB-3). |
| P2 | Harden subscription create idempotency, bounded scheduler scans, structured retries/metrics and event replay | Reduces duplicate plans and operational blind spots (SUB-6 and scheduler gaps). |
| P2 | Audit released mobile/web repositories and align all API/realtime/polling contracts | Current checkout cannot verify shipped client behavior. |

## 19. Phase-by-Phase Implementation Plan

**Phase 0 - Containment and contract freeze**

- Disable cross-booking STOMP subscriptions and the legacy customer refund state mutation.
- Publish canonical booking/payment/subscription/partner state machines and route schemas.
- Audit deployment schema against all required V migrations and document current live versions.

**Phase 1 - State integrity and durable effects**

- Add transactional outbox/event delivery for payment capture dispatch, booking realtime notifications, and partner notification side effects.
- Add idempotent DB claims for scheduled order and subscription visit dispatch.
- Implement scheduled order scan with configured pre-dispatch window, batch bounds, failure state, and retry/dead-letter operations.

**Phase 2 - Subscription operational consistency**

- Define assignment semantics (preference vs confirmed visit assignment), then make admin assignment create valid partner-visible work or preserve fallback dispatch.
- Implement replacement scope and stale request/order supersession.
- Make pause/cancel/reschedule synchronize visits and active offers; ensure next visit starts only when eligible.
- Gate subscription activation/dispatch on captured payment except for explicitly audited admin override.

**Phase 3 - Payment hardening**

- Enforce one active attempt per booking, reject new charge order after payment, validate captured amount/currency/order on webhook, and reconcile partial captures.
- Retire legacy local initiation/refund/status mutation or route through canonical payment/refund services.
- Add webhook and outbox dashboards/reconciliation alerts; keep webhook idempotency separate from dispatch idempotency.

**Phase 4 - Contract and client integration**

- Version the reschedule route and schemas; regenerate OpenAPI/Postman and contract tests.
- Verify released customer RN, partner RN, and admin web implementations against access token refresh, payment success/failure, accept expiry, service PIN completion, reconnect, and polling fallback.
- Select one production realtime topology (external broker/relay or authenticated API/SSE) and document exact connect/subscription destinations.

**Phase 5 - Release gates**

- MySQL integration tests with competing replicas/threads for accept, scheduled dispatch, subscription dispatch, cancellation/reschedule races and outbox replay.
- End-to-end test each full flow including payment capture/failure/refund, partner rejection/no partner, replacement, completion and audit trail.
- Staging rehearsal with delayed/duplicate webhooks, Redis outage, WebSocket loss, scheduler restart, MySQL failover, and reconciliation before production rollout.

## 20. Production Readiness Risks

### Already production-safe or strongly guarded

- Canonical instant flow does not intentionally dispatch before `PaymentStatus.SUCCESS`; acceptance rechecks payment.
- Razorpay checkout signature and fetched order/amount are checked on verify; webhook signatures and event deduplication/retry are implemented.
- Customer ownership checks are present on canonical booking/payment/subscription paths.
- Normal partner acceptance has request/order pessimistic locks and a MySQL unique accepted-request guard; a corresponding MySQL concurrency test exists.
- Partner service start/completion paths require customer PIN verification; visit completion is linked to order lifecycle.
- Subscription visit-to-order link and per-subscription/per-visit assignment unique constraints exist.

### Partially safe

- Instant payment-before-dispatch ordering: correct gate, but no durable recovery after payment commit (IB-1).
- Partner acceptance and order locking: tested on MySQL for selected concurrent path; expiry/error rollback branches and all scheduler/admin races remain unproven.
- Subscription payment activation, visit lifecycle, pause/resume/cancel and reschedule: APIs exist, but assignment/replacement/offer reconciliation is incomplete.
- Payment: robust canonical service plus unsafe legacy state mutation and incomplete active-attempt/capture validation.
- Realtime: event persistence and topic broadcasting exist, but private topic authorization, transaction ordering, and multi-instance delivery do not.

### Unsafe / not ready

- Scheduled one-time booking dispatch is not wired (SB-1).
- Arbitrary authenticated STOMP subscriber can receive another booking's lifecycle/ETA and another user's id-addressed notifications (RT-1).
- Payment capture can commit while instant dispatch is permanently skipped after a listener failure (IB-1).
- Admin per-visit assignment can suppress dispatch without creating partner work; replacement does not supersede active offers/work (SUB-3/SUB-4).
- Legacy refund route changes local status without a provider refund; paid bookings can get new provider orders using new idempotency keys (PAY-1/PAY-2).
- Released frontend behavior, app-to-backend endpoint consistency, and polling fallback are unverified because those app repositories are absent.

### Must be fixed first

1. STOMP booking/notification authorization.
2. Scheduled one-time dispatch path and idempotent pre-window scheduler.
3. Durable payment-capture dispatch handoff/reconciliation for instant bookings.
4. Subscription per-visit assignment/replacement and concurrent dispatch integrity.
5. Legacy refund/payment endpoint behavior and duplicate/mismatched capture protection.

### Safe to defer

Non-critical visual polish, copy/localization refinements, and broader reporting enhancements can be deferred. Do **not** defer the findings above, state-machine/contract consolidation, core recovery tests, or verification against actual released client apps. Client integration itself cannot be declared safe to defer if production clients call these APIs; first identify the shipped app versions and endpoint usage.

## Phase 2 Implementation Status (partial; 2026-10-04)

This section records only changes made and evidence observed in this implementation pass. The original audit findings above are preserved. This phase is **not complete** and the backend is **not production-ready**.

### Completed in this pass

| Finding / area | Status | Changes and evidence |
|---|---|---|
| RT-1 booking and notification STOMP topic authorization | FIXED for the covered destinations | `ChatStompAuthChannelInterceptor` now authenticates ADMIN and validates customer ownership or assigned-partner access for booking and ETA destinations; notification subscriptions are recipient-scoped; unknown `/topic/` destinations fail closed. `ChatService.hasBookingAccess` reads the authoritative order owner/assigned partner and checks actor type to prevent numeric ID collisions between customers and partners. Interceptor and service tests cover cross-customer, unrelated partner, ID collision, legitimate customer, assigned partner, and notification/admin role cases. |
| SUB-1 subscription payment dispatch window | PARTIALLY FIXED | `SubscriptionAutoDispatchService.dispatchForBooking`, scheduler dispatch, and retry now share `dispatchClaimedVisit`, which checks the configured window, active subscription/visit states, and successful payment. Unit coverage continues to include a future visit not being dispatched. There is no MySQL scheduler/payment-listener race test in this pass. |
| SUB-2 subscription visit dispatch duplicate prevention | PARTIALLY FIXED | `SubscriptionAutoDispatchService` obtains pessimistic locks on the subscription and visit before creating visit requests and validates payment and current visit state. Flyway V155 adds unique generated-key indexes for an active request per subscription visit and a pending order/partner request. This migration has not been run against a production-like MySQL dataset; pre-existing duplicate active requests will make it fail safely rather than delete or rewrite data. MySQL concurrency evidence is still missing. |
| SUB-3 admin per-visit assignment semantics | PARTIALLY FIXED | Admin per-visit assignment is now explicitly a preferred-partner record (`ADMIN_PREFERRED_PARTNER:` reason), not a confirmed assignment. The dispatcher tries the preference first and still allows fallback dispatch; pending offers are expired when a new preference is stored. The unit test verifies the preferred-partner semantic and existing dispatch tests verify fallback. End-to-end partner-visible work and visit completion are not covered. |

### Modified files in this pass

- `application-module/src/main/java/com/lexho/user_service/chat/config/ChatStompAuthChannelInterceptor.java`
- `application-module/src/main/java/com/lexho/user_service/chat/service/ChatService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/repository/SubscriptionMasterRepository.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionAutoDispatchService.java`
- `application-module/src/main/java/com/lexho/user_service/subscription/service/AdminSubscriptionManagementService.java`
- `application-module/src/test/java/com/lexho/user_service/chat/config/ChatStompAuthChannelInterceptorTest.java`
- `application-module/src/test/java/com/lexho/user_service/chat/service/ChatServiceTest.java`
- `application-module/src/test/java/com/lexho/user_service/subscription/service/SubscriptionAutoDispatchServiceTest.java`
- `application-module/src/test/java/com/lexho/user_service/subscription/service/AdminSubscriptionManagementServiceTest.java`
- `application-module/src/main/resources/db/migration/application-module/V155__dispatch_active_request_uniqueness.sql`

These files also contained pre-existing worktree edits before this pass; this list does not imply those earlier edits were authored here.

### Migration and executed tests

- Added, not yet applied: `V155__dispatch_active_request_uniqueness.sql`.
- Targeted application-module Maven tests after the final changes: 41 passed, 0 failures, 0 errors (10 STOMP authorization, 16 chat service, 12 admin subscription, 3 subscription dispatch unit tests).
- An expanded booking regression selection ran 77 tests but had 1 error in `PartnerRequestServiceTest.rejectShouldMarkInstantOrderFailedWhenNoRequestsRemain`: `Invalid order status transition from null to INSTANT_FAILED`. The other 76 selected tests passed; the failure remains unresolved.
- Full `-am` reactor test attempt was blocked before application-module tests because the existing `user-service` test compilation cannot resolve Testcontainers (`org.testcontainers.containers` and `org.testcontainers.junit.jupiter` are absent from that module's test dependencies).
- No MySQL-backed integration/concurrency tests, migration execution, full backend suite, or production-like data validation were completed.

### Targeted re-audit of original findings

| Finding | Status | Remaining gap |
|---|---|---|
| IB-1 durable payment-capture-to-dispatch recovery | NOT FIXED | Payment state and dispatch intent are not yet transactionally connected by a durable outbox/replay worker. |
| SB-1 scheduled one-time dispatch | NOT FIXED | No bounded, atomic scheduled-booking dispatch worker was implemented. |
| SB-2 canonical reschedule API | NOT FIXED | Alternate route/contract ambiguity remains. |
| SB-3 stale requests and dispatch state on reschedule | NOT FIXED | No atomic reschedule reconciliation or accept-vs-reschedule race test was added. |
| SUB-1 first visit dispatch window | PARTIALLY FIXED | One eligibility gate now covers payment event, scheduler, and manual retry; no durable scheduler/concurrency proof. |
| SUB-2 visit atomic claim | PARTIALLY FIXED | Pessimistic parent/visit locks and a DB unique active-request guard were added; migration execution and MySQL competing-worker evidence remain outstanding. |
| SUB-3 admin per-visit assignment | PARTIALLY FIXED | Preferred-partner model no longer suppresses the dispatcher in this service; operational end-to-end validation is outstanding. |
| SUB-4 partner replacement | NOT FIXED | Replacement still needs current/future scope semantics, stale-work invalidation, and historical integrity tests. |
| SUB-5 pause/cancel offer reconciliation | NOT FIXED | Lifecycle operations do not yet atomically supersede every active offer and reconcile scheduler eligibility. |
| SUB-6 subscription creation idempotency | NOT FIXED | No request idempotency key/unique constraint was implemented. |
| PAY-1 legacy customer refund mutation | NOT FIXED | Legacy route has not been retired or routed through the provider refund workflow in this pass. |
| PAY-2 duplicate provider order after paid booking | NOT FIXED | Payment retry/idempotency protections require further audit and tests. |
| PAY-3 capture amount/currency reconciliation | NOT FIXED | Provider capture webhook validation was not changed or proven in this pass. |
| RT-1 STOMP booking/notification authorization | FIXED for currently recognized private topics | The interceptor fails closed on unknown `/topic/` subscriptions and validates currently recognized booking/ETA/notification destinations; deployed destination inventory still requires client-version verification. |

### Remaining critical/high findings and next phase

Do not release this backend based on this partial phase. Highest priority remains durable payment-to-dispatch outbox/replay, scheduled one-time dispatch, canonical and race-safe reschedule, payment/refund/capture validation, subscription lifecycle/replacement reconciliation, and MySQL concurrency/migration verification. The first reactor test attempt also exposed an existing Testcontainers dependency gap in `user-service`; resolve the build setup before treating a full reactor test result as available.
