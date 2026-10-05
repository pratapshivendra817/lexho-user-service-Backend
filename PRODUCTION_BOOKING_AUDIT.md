# Lexho Production Booking Audit

**Audit date:** 2026-10-04  
**Scope:** Backend repository plus the customer, partner, and admin frontend trees referenced by `BOOKING_FLOWS_README.md`. This is a static/current-worktree audit, not a production-environment certification.

## 1. Executive Summary

**Overall status: FAIL - not production-ready.** In addition to the earlier payment, reschedule, and lifecycle fixes, this pass adds bounded scheduled-dispatch retries, transactional slot-capacity serialization, subscription-create idempotency, active/paid admin assignment gates, future-visit replacement validation, authenticated STOMP for customer booking updates, backend STOMP publication for partner dispatch request lifecycle events, partner dispatch topic authorization, and a live in-process partner STOMP delivery test. Focused backend and customer tests cover these changes.

Important risks remain. Razorpay idempotency keys are not reserved globally before the external gateway call; the backend now registers `/ws/partner/dispatch` as a STOMP endpoint, authorizes partner-specific dispatch subscriptions, and has a live authenticated STOMP delivery test, but the external partner app source is not present in this checkout for frontend-client verification; MySQL migration execution and multi-instance lock behavior have not been verified against a live database. Full production environment verification is incomplete.

Source changes were made after the user authorized implementation. Existing dirty-worktree changes were preserved. Migration V156 adds subscription idempotency fields and subscription/date uniqueness; existing production data must be checked for duplicate visit dates before applying it.

## 2. Instant Booking Audit

**Status: PARTIAL.** `LexhoExperienceController#createBooking` creates an instant booking pending payment and the capture listener is the authoritative dispatch trigger. Partner acceptance locks the request and order, checks request expiry, payment success, order state, partner dispatch eligibility, and ETA; dispatch uniqueness is also present in the current migrations.

The controller status/event behavior has focused tests, and client polling remains enabled if realtime fails. The route from payment capture to instant dispatch is not fully proven recoverable across process failure.

## 3. Scheduled Booking Audit

**Status: PARTIAL.** Slot listing uses a date window, configured slot size/capacity, and existing active order counts. Booking creation and rescheduling serialize capacity checks and writes by locking the singleton cart configuration row for the duration of the transaction; this is safe but coarse-grained and still needs MySQL integration verification. Customer rescheduling uses the slot-aware `POST`; the legacy `PUT` alias resolves a listed slot and delegates to the same ownership/status/payment/capacity validator (F-2). Dispatch and partner acceptance require backend-confirmed payment. A periodic scan reclaims due, paid scheduled orders, records retry attempts, retries empty dispatches, and marks orders unavailable after five attempts.

Capacity is serialized in application transactions rather than represented by a database reservation row; partner schedule-conflict prevention across all scheduled assignment paths is still not proven.

## 4. Subscription Booking Audit

**Status: PARTIAL.** The model separates `SubscriptionMaster`, `SubscriptionVisit`, per-visit partner requests, and visit orders. Visits are linked to unique booking IDs in migration V139; auto-dispatch locks the subscription and visit, checks active/paid state and dispatch eligibility, and avoids existing active requests/assignments. Partner visit acceptance also locks the visit and checks active subscription, eligible visit state, and successful master payment.

Create accepts a customer idempotency key, stores a canonical request hash, serializes keyed creates on the customer's address rows, and replays matching requests; a changed payload with the same key is rejected (F-7). V156 enforces unique customer/key and subscription/visit-date pairs, subject to a pre-deployment duplicate-data check. Pause, resume, and cancel use parent locking and repeated target-state operations are idempotent (F-8); cancellation covers future `SCHEDULED`/`RESCHEDULED` visits without modifying accepted/active/completed visits (F-9). Visit rescheduling locks the visit and parent and treats a repeat of the same date as a no-op; cross-subscription capacity and partner conflicts still need broader validation (F-10).

## 5. Customer App Audit

**Status: PARTIAL.** `PaymentScreen` requires `verified=true` and `paymentStatus=SUCCESS` from the backend before recording success or navigating (F-3). Scheduled rescheduling resolves the chosen local time to a currently listed slot and uses the canonical slot-aware `POST` route (F-2). Instant lifecycle connects to the authenticated customer STOMP endpoint and subscribes to the booking topic; authoritative HTTP polling remains enabled as fallback (F-4).

The frontend is outside the backend repository and has its own dirty worktree. Customer payment and rescheduling tests were updated; the selected customer tests pass.

## 6. Partner App Audit

**Status: PARTIAL.** Partner dispatch request polling and retries have focused tests. Request acceptance is backed by server-side validation, including successful payment for scheduled requests; the app does not own assignment authority. The backend now registers `/ws/partner/dispatch` as a STOMP endpoint, authorizes `/topic/partner/dispatch/{partnerId}` for the matching partner, keeps `/topic/partner/dispatch` admin-only, and publishes request lifecycle payloads to both destinations (F-11). A live in-process STOMP test verifies authenticated partner connection, subscription, backend publication, and payload receipt; polling remains the recovery path for network/client failures.

No external partner app source exists in this checkout, so frontend-client code was not changed here.

## 7. Admin Assignment Audit

**Status: PARTIAL.** Admin subscription assignment requires an ACTIVE subscription and successful master payment, then validates partner profile, skill, operational eligibility, distance/ETA, and schedule conflicts. Replacement targets all future scheduled/rescheduled visits, preserves accepted/active/completed visits and historical assignment data, expires pending requests, and relies on the canonical per-visit auto-dispatch path; a selected replacement remains a preference subject to live eligibility at dispatch.

Admin-triggered general order dispatch is guarded by the backend payment gate. Subscription-level assignment remains a preference operation, not proof of final assignment until a visit dispatch request is accepted. Service tests cover validation and replacement behavior; no admin UI end-to-end test was run.

## 8. Payment/Razorpay Audit

**Status: PARTIAL.** The backend requires an idempotency header for payment-order creation, has unique payment idempotency/order/payment references, validates checkout signatures, fetches the payment, compares order/amount/currency, records webhook event IDs, and ignores duplicate captured payments. Webhook retry support is present. Payment service/controller and webhook focused tests passed.

Same-booking payment-order retries now lock the booking and recheck the idempotency key before contacting Razorpay; idempotency keys are also rejected when reused for a different booking (F-5). This does not provide a durable cross-booking reservation before the external gateway call, so global concurrent key reuse remains a risk. Checkout verification now uses the existing pessimistic payment-row lookup used by webhook processing (F-6). The customer only reports success after the backend affirms capture (F-3).

## 9. Dispatch & Partner Matching Audit

**Status: PARTIAL.** Shared eligibility checks include partner operational/shift status and service skills. Instant assignment is guarded by order/request locks and payment state. Subscription dispatch checks paid state and eligible visit state. V154/V155 add active-request and accepted-request database constraints.

Dispatch orchestration and `PartnerRequestService#accept` now both reject unpaid scheduled orders (F-1). A due-booking scheduler revalidates payment, state, existing assignment, and scheduling window under the order lock before calling the shared dispatch path. End-to-end global partner schedule-conflict behavior remains unproven.

## 10. Scheduler Audit

**Status: PARTIAL.** Subscription auto-dispatch runs periodically and locks subscription/visit rows before claiming work; partner request expiry and instant-search timeout schedulers exist. Payment webhooks have a retry scheduler.

Subscription auto-dispatch and partner request expiry remain periodic. A scheduled-booking scanner selects paid, unassigned eligible orders and uses order locking; dispatch attempts are persisted as booking events, retried on later scans, and terminated after five failed partner searches. Production multi-instance/timezone behavior and recovery against live MySQL were not tested.

## 11. Concurrency & Idempotency Audit

**Status: PARTIAL.** Partner acceptance and subscription visit assignment use pessimistic locks; payment verification, subscription management, and customer booking rescheduling now use row locks. DB uniqueness exists for several payment and dispatch relationships. Remaining gaps:

- Cross-booking reuse of an idempotency key can still race before external Razorpay order creation (F-5).
- Checkout verification now locks the transaction row; no concurrent integration test was run (F-6).
- Scheduled capacity checks serialize with a pessimistic lock on the configuration row; multi-instance MySQL behavior remains unverified (F-6).
- Subscription create locks customer address rows for keyed requests and enforces unique customer/key and subscription/date constraints in V156; migration application and concurrent DB tests remain outstanding (F-7, F-10).
- Customer reschedules serialize capacity checks and writes using the configuration lock and booking lock (F-2, F-6).

## 12. API Contract Audit

**Status: PARTIAL.** The primary subscription create path `/api/subscription/create` and compatibility alias `/api/subscription/bookings` both exist; the customer caller retries only on 404/405. Payment create-order and verification routes match the customer caller. The admin visit-partner `PATCH` route matches its backend mapping.

`POST /api/bookings/{id}/reschedule` with `{date, slot}` is the canonical customer route. The legacy `PUT /api/bookings/reschedule/{id}` remains as a compatibility alias, maps the requested start time to a listed slot, and delegates to the same shared policy helper rather than maintaining separate business rules (F-2).

## 13. WebSocket/Realtime Audit

**Status: PARTIAL for customer booking state; VERIFIED for backend partner dispatch STOMP.** Backend uses authenticated STOMP endpoints and broker destinations. Customer instant lifecycle sends STOMP `CONNECT` and `SUBSCRIBE` frames with its access token, reconnects with bounded backoff, invalidates the authoritative booking query on matching or malformed messages, and retains HTTP polling fallback (F-4). No live authenticated customer socket integration test was run.

Partner dispatch now has a backend STOMP endpoint at `/ws/partner/dispatch`, request lifecycle events are emitted when requests are created, accepted, rejected, or expired, and the backend live STOMP route is verified by `PartnerDispatchStompE2ETest`. The test opens a real WebSocket, sends a partner JWT in STOMP `CONNECT`, subscribes to `/topic/partner/dispatch/{partnerId}`, publishes via `PartnerDispatchRealtimeService`, and receives the dispatch payload. The external partner app source is not present in this checkout, so deployed client-version compatibility still needs release verification; polling still provides eventual recovery (F-11).

## 14. Database Integrity Audit

**Status: PARTIAL.** The current migration set includes unique payment idempotency/order/payment references (V58), unique subscription visit booking linkage (V139), active dispatch uniqueness constraints (V154/V155), and V156 customer idempotency plus subscription/date uniqueness.

V156 has not been applied or validated against production MySQL; existing duplicate `(subscription_id, visit_date)` rows could block rollout and must be checked before deployment. Slot capacity is serialized via row locking rather than a reservation table. Payment verification and subscription management use existing pessimistic locks; no `@Version` fields were added.

## 15. Security Audit

**Status: PARTIAL / NOT A PENETRATION TEST.** Payment verification is server-side; ownership is checked for payment retrieval/verification and customer booking/visit paths; partner assignment has server-side operational/skill checks. WebSocket destinations have an inbound STOMP auth interceptor and origin configuration.

Customer booking realtime now performs the STOMP authentication/subscription exchange, but live customer authorization and message delivery were not tested. Partner dispatch STOMP authorization and backend message delivery are covered by focused tests. Abuse behavior for all admin routes, production JWT claims, deployed frontend socket compatibility, and Razorpay secrets were not penetration-tested. No credentials or payment secrets were included in audit logging.

## 16. Error Recovery Audit

**Status: PARTIAL.** Webhook retries, request expiry, dispatch timeout scans, and frontend partner polling are implemented. Payment failure is recorded on the backend; subscription dispatch rechecks payment and parent status.

Customer payment no longer presents unverified payment as successful (F-3); polling remains active alongside STOMP (F-4); scheduled dispatch retries and terminates boundedly; parent cancellation includes future rescheduled visits (F-9). Crash recovery for every payment/dispatch/scheduler boundary was not proven with production-database integration tests.

## 17. Automated Test Results

**Status: PARTIAL COVERAGE.**

| Verification | Result |
|---|---|
| Focused backend payment tests (`PaymentServiceTest`) | 13 passed after payment-lock changes |
| Focused booking/dispatch/subscription tests (`DispatchOrchestrationServiceTest`, `PartnerRequestServiceTest`, `SubscriptionBookingServiceTest`, `LexhoExperienceControllerInstantTest`, `SlotBookingPaymentContractTest`) | 47 passed after updates |
| Earlier selected backend dispatch/admin/subscription run | 64 passed; 0 failures/errors |
| Selected backend aggregate including `PaymentServiceTest` | 60 passed |
| Latest targeted backend dispatch/admin/subscription regression run | 45 passed; 0 failures/errors |
| Partner realtime dispatch hook tests | 13 passed |
| Backend partner dispatch STOMP publisher and related request/dispatch regression tests | 39 passed; 0 failures/errors; Checkstyle 0 violations |
| Partner dispatch authenticated backend STOMP delivery (`ChatStompAuthChannelInterceptorTest`, `PartnerDispatchStompE2ETest`) | 14 passed; 0 failures/errors; Checkstyle 0 violations |
| Customer `PaymentScreen`, `bookingExtrasApi`, and `useInstantBookingLifecycle` tests | 34 passed |
| Customer STOMP lifecycle (including token retrieval retry) and subscription API idempotency tests | 17 passed |
| Backend compile and Checkstyle for payment/application modules | Passed; 0 Checkstyle violations |
| Backend SpotBugs dependency install/compile (`order-service` reactor) | Passed; 0 SpotBugs findings |
| Slot/payment contract integration test | 2 passed after aligning the test fixture with the controller's pessimistic booking lookup |
| Full reactor tests, SonarQube, JaCoCo threshold, live MySQL migration validation | Not run or not verified |

## 18. Remaining Risks

1. Payment idempotency keys are not globally reserved before the external Razorpay order call; concurrent cross-booking reuse remains possible.
2. Migration V156 needs preflight duplicate-data review and MySQL validation before rollout.
3. Partner backend STOMP endpoint/topic publication and authenticated backend delivery are verified; deployed partner client compatibility still needs release verification because the external app source is not present in this checkout. Polling remains the fallback.
4. Schedule conflict detection and coarse-grained slot locking need multi-instance MySQL integration coverage; scheduler timing/timezone behavior is not production-verified.
5. Full frontend builds, SonarQube, JaCoCo thresholds, production JWT/WebSocket authorization, and complete end-to-end flows are unverified.
6. Existing worktree changes predated this implementation; unrelated changes were preserved.

## 19. Production Readiness Score

**74/100 - FAIL.** Scheduled dispatch retry/exhaustion, slot-capacity serialization, subscription-create idempotency, customer STOMP integration, backend partner dispatch STOMP publication and live backend delivery, and admin assignment validation have been implemented with targeted tests. The system is not production-ready because deployed partner-client verification, global payment-key reservation, production MySQL/multi-instance verification, and full integration evidence remain incomplete.

### Finding Register

| ID | Severity | Component | File and method/class | Problem | Root cause | Fix applied | Test added | Verification result |
|---|---|---|---|---|---|---|---|---|
| F-1 | HIGH | Scheduled dispatch / Partner | `application-module/src/main/java/com/lexho/user_service/partnerrequest/service/PartnerRequestService.java` - `accept`; `.../dispatch/service/DispatchOrchestrationService.java` - `dispatchOrder` | Scheduled bookings could be dispatched/accepted while unpaid. | Payment guard was instant-only. | Added scheduled payment gates and a paid scheduled dispatch method invoked by capture and recovery scheduler. | Yes | Dispatch and partner request tests pass; no live payment-to-dispatch integration run. |
| F-2 | HIGH | Scheduled reschedule API | `application-module/src/main/java/com/lexho/user_service/lexho/controller/LexhoExperienceController.java` - `rescheduleBooking`, `rescheduleBookingV2`; customer `bookingExtrasApi.ts` | Two routes previously had divergent rules and active UI used date-only `PUT`. | Legacy handler previously changed scheduling without sharing strict slot policy. | Canonical slot-aware `POST`; legacy route now resolves a listed slot and delegates to the locked shared validator. | Yes | Customer API contract test passes; screen mapping and controller slot tests were not run. |
| F-3 | HIGH | Customer payment | Customer `PaymentScreen.tsx` - payment mutation | UI could navigate after an HTTP-success verification with `verified=false`. | Resolved response was not inspected. | Require `verified=true` and `paymentStatus=SUCCESS` before success analytics, pending-state clear, and navigation. | Yes | Negative verification test and customer tests pass. |
| F-4 | HIGH | Customer realtime | `apps/customer/legacy-mobile/src/hooks/useInstantBookingLifecycle.ts`; `application-module/src/main/java/com/lexho/user_service/notification/config/NotificationWebSocketConfig.java` | Customer socket previously used a raw protocol against a STOMP endpoint. | Client did not authenticate or subscribe using STOMP frames. | Send authenticated STOMP `CONNECT` and per-booking `SUBSCRIBE`, reconnect with backoff, and retain polling fallback. | Yes | Customer lifecycle tests pass (including STOMP contract); no live authenticated socket E2E test. |
| F-5 | HIGH | Payment idempotency | `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java` - `createOrder` | Concurrent same-booking retries could contact Razorpay before noticing an existing idempotency record; cross-booking key reuse is rejected. | Idempotency recheck was only before booking serialization. | Recheck after locking the booking and reject key reuse for a different booking. No durable global reservation was added. | Partial | Payment service tests pass; no concurrent gateway simulation. |
| F-6 | HIGH | Payment / slot concurrency | `payment-service/src/main/java/com/lexho/payment/service/PaymentService.java` - `verify`; `application-module/src/main/java/com/lexho/user_service/customer/service/CustomerCartSlotService.java` - `lockSlotCapacity` | Verification/webhook race was mitigated; scheduled capacity checks previously raced between concurrent bookings/reschedules. | Verify used a non-locking lookup; slot capacity was a count-before-write operation. | Verification locks the transaction row; scheduled booking/reschedule locks the singleton configuration row through the write transaction. | Yes | Focused payment and booking tests pass; MySQL multi-instance lock behavior is not verified. |
| F-7 | HIGH | Subscription creation | `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionBookingService.java` - `create`; customer `subscriptionBookingApi.ts` | Retries could create duplicate subscription/payment/visit sets; concurrent same-key requests could race. | Create previously lacked request idempotency. | Client sends a retained key; backend stores normalized key/hash, locks customer address rows before lookup/create, replays identical requests, rejects changed payloads, and V156 adds uniqueness. | Yes | Backend replay/hash tests and customer API contract test pass; no concurrent MySQL test. |
| F-8 | MEDIUM | Subscription lifecycle | `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionBookingService.java` - `pause`, `resume`, `cancel` | Concurrent operations and repeats previously were not safely handled. | Mutations did not use the parent `findByIdForUpdate` lock and repeated targets were rejected. | Parent locking and idempotent same-target behavior added. | Yes | Subscription service tests pass; no concurrent DB test. |
| F-9 | MEDIUM | Subscription cancellation | `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionBookingService.java` - `cancel`; `.../subscription/repository/SubscriptionVisitRepository.java` - `cancelFutureScheduled` | Future `RESCHEDULED` visits previously survived parent cancellation. | Cancellation bulk update filtered only `SCHEDULED`. | Cancellation now conditionally cancels both future pending visit states and preserves accepted/active/completed visits. | Yes | Cancellation service test verifies both states. |
| F-10 | HIGH | Subscription visit reschedule | `application-module/src/main/java/com/lexho/user_service/subscription/service/SubscriptionBookingService.java` - `rescheduleVisit` | Concurrent reschedules could previously choose the same visit date; cross-subscription partner/slot conflict coverage remains incomplete. | Visit was loaded without `FOR UPDATE`; duplicate-date check was read-before-write. | Lock visit and parent rows; identical repeat date is a no-op; V156 adds unique `(subscription_id, visit_date)`. | Yes | Service tests pass; migration and DB concurrency validation remain outstanding. |
| F-11 | MEDIUM | Partner realtime | Backend `NotificationWebSocketConfig.java`, `ChatStompAuthChannelInterceptor.java`, `PartnerDispatchRealtimeService.java`, `DispatchOrchestrationService.java`, `PartnerRequestService.java`; external partner client source not present in this checkout | Partner dispatch realtime had no backend endpoint/topic for the audited `/ws/partner/dispatch` contract, and the STOMP auth guard did not authorize partner dispatch subscriptions. | Backend endpoint/topic support and destination authorization were incomplete for partner dispatch realtime. | Registered `/ws/partner/dispatch` as a STOMP endpoint; publish request created/accepted/rejected/expired payloads to partner-specific and admin broadcast topics; authorize partners only for `/topic/partner/dispatch/{ownPartnerId}` and keep `/topic/partner/dispatch` admin-only. Polling remains the fallback for network/client failures. | Yes | 14 latest targeted STOMP tests passed, including live authenticated backend delivery in `PartnerDispatchStompE2ETest`; focused publisher/request/dispatch tests also pass. |
| F-12 | MEDIUM | Admin assignment | `application-module/src/main/java/com/lexho/user_service/subscription/service/AdminSubscriptionManagementService.java` - `assignPartner`, `replacePartner`, `assignVisitPartner` | Admin preferences previously lacked direct paid/active and schedule-conflict validation and replacement did not cover every future visit. | Master preference was decoupled from visit eligibility and assignment rows were not locked. | Require active/paid subscription; lock master/visit; validate skill, availability, distance/ETA and schedule conflicts; expire pending requests for every eligible future visit; preserve accepted/active/completed visits; auto-dispatch uses per-visit preferences through the shared path. | Yes | Targeted admin service tests pass; UI and live partner-dispatch integration not run. |
| F-13 | HIGH | Scheduled dispatch retry | `application-module/src/main/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationService.java` - `dispatchPaidScheduledBooking`, `finishDispatch`; `ScheduledBookingDispatchScheduler` | A due scheduled booking with no eligible partner could remain in search without a bounded retry/recovery path. | Initial dispatch marker prevented duplicate claims, but empty searches had no durable attempt policy. | Persist attempt events, retry from `PARTNER_SEARCH` on scheduler scans, stop after five attempts with an unavailable reason, and exclude terminal orders from recovery. | Yes | Dispatch tests pass, including retry and exhaustion; no live multi-instance scheduler test. |
