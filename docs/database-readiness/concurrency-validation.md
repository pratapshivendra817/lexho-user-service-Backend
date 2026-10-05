# Concurrency Validation

Status date: August 15, 2026

## Executed

- `PASS` payment transaction duplicate-idempotency race:
  - `payment-service/src/test/java/com/lexho/payment/repository/PaymentPersistenceIntegrityTest.java`
- `PASS` payment refund duplicate-idempotency race:
  - `payment-service/src/test/java/com/lexho/payment/repository/PaymentPersistenceIntegrityTest.java`
- `PASS` partner payout duplicate race:
  - `application-module/src/test/java/com/lexho/user_service/lexhopartner/service/PartnerSettlementPersistenceTest.java`
- `PASS` dispatch conflicting reassignment guard:
  - `application-module/src/test/java/com/lexho/user_service/dispatch/service/DispatchOrchestrationServiceTest.java`
- `PASS` real-MySQL dispatch/provider assignment row-lock race:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlConcurrencyIT.java`
  - competing partner acceptance attempts use `SELECT ... FOR UPDATE`; only one partner assignment and one active-job increment commits
- `PASS` real-MySQL multi-thread terminal booking state-transition race:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlConcurrencyIT.java`
  - competing `SERVICE_COMPLETED` and `CANCELLED` terminal transitions serialize; only one terminal transition event commits
- `PASS` real-MySQL concurrent business-flow DB load profile:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlConcurrencyIT.java`
  - 8 worker threads executed 120 booking/payment/dispatch-request/payout flows with zero failures and asserted backend latency thresholds

## Hardening Added

- `DispatchOrchestrationService.markPartnerAccepted(...)` now rejects conflicting reassignment after row lock acquisition.
- `PartnerSettlementService.ensurePayout(...)` now treats duplicate insert races as idempotent and relies on DB uniqueness.
- `payouts.reference_booking_id` now has a database unique constraint via `V109__payout_reference_booking_uniqueness.sql`.
- `ApplicationMysqlConcurrencyIT` now creates and resets the isolated `lexho_application_concurrency_test`
  database before real-MySQL race/load execution.

## Verification Command

```powershell
$env:IT_DB_URL='jdbc:mysql://127.0.0.1:3306/lexho_application_concurrency_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
.\mvnw.cmd -B -ntp -pl application-module "-Dlexho.it.mysql.concurrency=true" "-Dtest=ApplicationMysqlConcurrencyIT" test
```

Result on August 15, 2026:

- `PASS`
- tests run: `3`
- failures: `0`
- errors: `0`
- skipped: `0`

## Remaining Gaps

- No remaining isolated-MySQL concurrency gap for the listed booking state-transition, dispatch/provider assignment,
  payment idempotency, refund idempotency, payout, settlement, and backend business-flow DB workload scenarios.
- Full internet-facing production traffic behavior still needs staging or production observability because this proof is
  isolated backend/database validation, not mobile/web frontend device testing.
