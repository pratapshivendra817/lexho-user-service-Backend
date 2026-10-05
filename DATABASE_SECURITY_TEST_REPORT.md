# Database & SQL Injection Security Report

## Executive Summary

Current automated security gate: PASS as of August 14, 2026.

Historical database-readiness status: PARTIAL for environment-only production checks that cannot be proven from the local repository session.

See `SECURITY_PRODUCTION_TEST_REPORT.md` for the latest Customer, Partner, and Admin production security closure evidence.

The codebase was reviewed across controller -> validation/DTO -> service -> transaction -> repository -> JPA/JDBC -> MySQL paths, with emphasis on `application-module` and the multi-module Spring Boot reactor.

No confirmed SQL injection vulnerability was found in the reviewed business query paths. The default Maven test suite now runs in an isolated mode and completed successfully on August 5, 2026 with `.\mvnw.cmd test`.

The result remains PARTIAL rather than PASS because some production-relevant areas are still not fully exercised in an isolated MySQL environment, especially concurrency/race-condition coverage and broader live MySQL migration/repository execution.

## Test Environment

Database: H2 in MySQL-compatibility mode for most automated tests; isolated local MySQL 8.0 for selected module-specific integration tests.

Test strategy:
- Static repository and query audit
- Repository slice tests
- Service transaction tests
- Flyway configuration review
- Isolated local MySQL connectivity verification
- Isolated local MySQL Flyway migration execution
- Isolated local MySQL repository execution
- Full multi-module Maven test execution
- Explicit safeguards against accidental production-like database usage in opt-in MySQL tests

Modules tested:
- `common-lib`
- `auth-service`
- `admin-module`
- `user-service`
- `catalog-module`
- `order-service`
- `payment-service`
- `partner-module`
- `notification-module`
- `application-module`
- `coverage-report`

## Database Tests

Total: 1314
Passed: 1308
Failed: 0
Skipped: 6

Source of totals: current Surefire XML reports after the successful root `.\mvnw.cmd test` run on August 5, 2026, excluding the known stale `LocalMySqlConnectivityIT` historical artifact from a prior unrelated run.

## SQL Injection Audit

Files scanned:
- Tracked `*.java`
- `*.sql`
- `*.properties`
- `*.yml`
- `*.yaml`
- Root and module `pom.xml`

Queries reviewed:
- Spring Data derived queries
- JPQL `@Query`
- native `@Query(... nativeQuery = true)`
- `JdbcTemplate` writes
- health-check JDBC `Statement` usage
- dynamic sort and filter handling in admin/search services

Confirmed vulnerabilities: 0

Fixed vulnerabilities: 0 confirmed SQL injection defects in business logic

Remaining risks:
- A few MySQL-specific integration checks are now explicit opt-in rather than part of the default suite
- Broad malicious-input API coverage is still incomplete outside the tested repository/service paths
- Concurrency-driven data integrity risks are not fully covered by executed tests

## SQL Injection Tests

Tested endpoints/components and results:
- `SupportTicketRepository` search path: malicious quote/comment/boolean-style inputs treated as literal search text, no query broadening
- `PaymentWebhookEventRepository.search(...)`: malicious quote/boolean-style search text treated as literal input, no result broadening
- `AdminUserManagementControllerIntegrationTest`: authenticated API request with malicious search/name/email/sort input returned zero rows and did not alter database state
- `ServiceableAreaService` JDBC mirror write: bind-parameter execution only, no user-controlled SQL structure
- Dynamic sort handling reviewed in admin/order/user/partner services: allowlist-based mappings observed, no direct request value interpolation confirmed
- Hardcoded JDBC health checks in [application-module/src/main/java/com/lexho/user_service/admin/service/AdminMonitoringService.java](/C:/Users/shivendra/Downloads/lexho-user-service-master/application-module/src/main/java/com/lexho/user_service/admin/service/AdminMonitoringService.java:83) and [application-module/src/main/java/com/lexho/user_service/health/HealthController.java](/C:/Users/shivendra/Downloads/lexho-user-service-master/application-module/src/main/java/com/lexho/user_service/health/HealthController.java:70): SAFE, no user input

## Repository Tests

Repositories tested and notable scenarios:
- `SupportTicketRepository`: filtered search behavior, SQL metacharacters handled as literals
- `PaymentTransactionRepository`: duplicate idempotency key rejected at the database level
- `PaymentWebhookEventRepository`: duplicate event/idempotency protection and malicious search text regression coverage
- `SectionRepository`/section entities: entity mapping and reserved-column compatibility
- `OrderRepository`: existing repository suite retained; default execution no longer depends on unsafe local MySQL defaults
- `NotificationRepository`: repository test converted to isolated H2 MySQL-mode execution
- Existing application-module repository suites also exercised paging, filtering, and custom query paths through the successful reactor run

## Constraint Tests

Constraints verified:
- JPA/Hibernate schema validation enabled in app config (`ddl-auto=validate`)
- Flyway-managed schema locations reviewed
- Unique and not-null behavior exercised through existing repository/service tests
- `customers.phone_number` uniqueness verified by `UserPersistenceIntegrityTest`
- `customers.email` uniqueness verified by `UserPersistenceIntegrityTest`
- `call_log.session_id` uniqueness verified by `UserPersistenceIntegrityTest`
- `payment_transactions.idempotency_key` uniqueness verified by `PaymentPersistenceIntegrityTest`
- `call_session`/`call_log` tables and FK/unique contracts verified on isolated local MySQL by `CallingFlywayMigrationTest`
- `orders` repository queries and pessimistic-lock lookup verified on isolated local MySQL by `OrderRepositoryTest`
- Reserved-column mapping corrected for `SectionItem.value`
- Safe isolated test-db guards added to MySQL opt-in tests

NOT TESTED:
- Full live MySQL constraint matrix for every business-critical entity
- Exhaustive FK/cascade/orphan-removal checks for every parent-child relationship named in the request

## Transaction Tests

Transaction/rollback scenarios:
- `ServiceableAreaService.createRollsBackWhenMirroredRuleWriteFails`: verified parent write rolls back when mirror-table JDBC write fails
- `UserPersistenceIntegrityTest.transactionRollbackRemovesSessionAndLogTogether`: verified a transactional session+log write leaves no partial commit after failure
- Existing service-layer tests across modules executed in the full suite

NOT TESTED:
- Full end-to-end rollback scenarios for booking creation, payment confirmation, refunds, payouts, and provider assignment
- Checked-exception rollback behavior where business rules depend on it

## Concurrency Tests

Race-condition scenarios and results:
- `PaymentPersistenceIntegrityTest.concurrentDuplicateIdempotencyKeyAllowsOnlyOneCommittedTransaction`: concurrent duplicate payment idempotency key attempt produced one committed row and one rejected write
- Static review performed for duplicate-write and state-transition risk areas

NOT TESTED:
- Simulated concurrent booking creation
- Double refund processing with live persistence contention
- Duplicate payout creation
- Lost-update/idempotency contention tests

## Flyway Tests

Migration status:
- Flyway is configured in `application-module`
- Migration source trees reviewed
- Validation-on-migrate is enabled

Fresh DB migration:
- PARTIAL
- Static migration checks and application-module Flyway-oriented tests exist
- Clean-from-zero MySQL migration was executed successfully for `user-service` calling tables on isolated schema `lexho_user_flyway_test`
- Full clean-from-zero MySQL migration across all relevant modules remains incomplete

Upgrade concerns:
- Existing applied production migrations were not rewritten
- If future schema fixes are needed, add new migrations only

## Performance Findings

N+1:
- No confirmed N+1 defect was fixed in this pass
- Several repository/service paths should still be profiled under real workload before a production-readiness claim

Pagination:
- Dynamic paging/sorting paths exist and were reviewed in core admin/search flows
- No confirmed unsafe unbounded user-controlled pagination path was fixed here

Indexes:
- Reviewed conceptually via entities/migrations and common query shapes
- No blind index additions were made

Slow-query risks:
- Native and JPQL query performance was reviewed statically, not benchmarked
- Real MySQL `EXPLAIN`/slow-query validation is NOT TESTED

## Security Findings

### 1. MEDIUM
Severity: MEDIUM
File: `user-service/src/test/java/com/lexho/user/integration/CallingFlywayMigrationTest.java`
Line: 14, 26-27
Issue: The test suite previously depended on a default local MySQL configuration and could run against a non-isolated developer database.
Risk: Unsafe database targeting during test execution.
Fix: Added explicit opt-in gating with `@EnabledIfSystemProperty` and a hard requirement that `IT_DB_URL` contains `lexho_user_flyway_test`.
Verification: Root `.\mvnw.cmd test` completed successfully without requiring a local MySQL instance.

### 2. MEDIUM
Severity: MEDIUM
File: `order-service/src/test/java/com/lexho/booking/repository/OrderRepositoryTest.java`
Line: 33, 51-52
Issue: The repository test previously assumed a local MySQL database.
Risk: Non-deterministic and unsafe default test execution against an uncontrolled database.
Fix: Added explicit opt-in gating and enforced isolated DB naming via `lexho_order_test`.
Verification: Root `.\mvnw.cmd test` completed successfully with the test skipped unless intentionally enabled.

### 3. MEDIUM
Severity: MEDIUM
File: `notification-module/src/test/java/com/lexho/notification/repository/NotificationRepositoryTest.java`
Line: 27
Issue: The repository test depended on local MySQL.
Risk: Default suite instability and unsafe environment coupling.
Fix: Moved the test to in-memory H2 with `MODE=MySQL` and added `com.h2database:h2` to `notification-module/pom.xml`.
Verification: `notification-module` passed in the final root test run.

### 4. INFO
Severity: INFO
File: `application-module/src/main/java/com/lexho/user_service/admin/serviceablearea/service/ServiceableAreaService.java`
Line: 129, 157
Issue: The mirror-table write used a MySQL-specific upsert path that blocked isolated cross-database test execution.
Risk: Reduced testability and weaker rollback verification in isolated environments.
Fix: Replaced the write with parameterized `UPDATE` then fallback `INSERT`, preserving behavior while enabling the transaction test.
Verification: `ServiceableAreaService` tests passed, including rollback-on-failure coverage.

### 5. INFO
Severity: INFO
File: `auth-service/src/test/java/com/lexho/auth/entity/RefreshTokenTest.java`
Line: 46
Issue: The test expected a raw token in `toString()` instead of the redacted security behavior already implemented in production code.
Risk: False-negative build failure and drift from secure logging expectations.
Fix: Updated the assertion to expect `token='[REDACTED]'`.
Verification: `auth-service` passed in the final root test run.

### 6. MEDIUM
Severity: MEDIUM
File: `payment-service/src/test/java/com/lexho/payment/repository/PaymentPersistenceIntegrityTest.java`
Line: 61-103
Issue: Critical payment idempotency behavior previously relied on service logic and unique constraints without a concurrent database race test.
Risk: Duplicate payment-transaction creation under concurrent requests could remain unverified.
Fix: Added a database-backed concurrent insert test for duplicate `idempotency_key` values.
Verification: One transaction committed, one duplicate write was rejected, and the final table state contained exactly one row.

### 7. INFO
Severity: INFO
File: `user-service/src/test/java/com/lexho/user/repository/UserPersistenceIntegrityTest.java`
Line: 40-76
Issue: Core user/call persistence invariants were not previously covered by a dedicated database-slice test.
Risk: Silent drift in uniqueness or transactional behavior.
Fix: Added direct database tests for duplicate phone/email, duplicate call-log session IDs, and rollback of session+log writes.
Verification: The new slice test passed in module and root runs.

### 8. INFO
Severity: INFO
File: `user-service/src/test/java/com/lexho/user/controller/AdminUserManagementControllerIntegrationTest.java`
Line: 24-89
Issue: API-level malicious-input coverage previously stopped at repository/service tests for user search behavior.
Risk: Regressions in controller -> validation -> service -> specification -> database wiring could go undetected.
Fix: Added a Spring Boot + MockMvc + H2 integration test hitting `/api/admin/users` with malicious search/name/email/sort input and a legitimate apostrophe-containing search term.
Verification: The test passed and confirmed no result broadening or unintended writes.

### 9. INFO
Severity: INFO
File: `user-service/src/test/java/com/lexho/user/integration/CallingFlywayMigrationTest.java`
Line: 13-46
Issue: Fresh isolated MySQL migration execution for the calling schema needed real runtime verification.
Risk: Flyway scripts could validate statically but still fail on live MySQL execution.
Fix: Executed the guarded MySQL Flyway test against isolated schema `lexho_user_flyway_test`.
Verification: `flyway.clean()` and `flyway.migrate()` completed successfully and schema contracts were asserted on MySQL 8.0.

### 10. INFO
Severity: INFO
File: `order-service/src/test/java/com/lexho/booking/repository/OrderRepositoryTest.java`
Line: 29-95
Issue: Order repository behavior on live MySQL remained opt-in and unexecuted.
Risk: Native and JPQL repository behavior could diverge from H2 coverage.
Fix: Executed the guarded MySQL repository test against isolated schema `lexho_order_test`.
Verification: Search, recent-order lookup, revenue aggregation, monthly revenue query, and `findByIdForUpdate` all passed on MySQL 8.0.

## Files Modified

- `application-module/src/main/java/com/lexho/user_service/admin/serviceablearea/service/ServiceableAreaService.java`  
  Replaced MySQL-specific mirror upsert with parameterized update/insert flow for isolated testing and rollback verification.
- `application-module/src/main/java/com/lexho/user_service/section/entity/SectionItem.java`  
  Escaped reserved column name `value`.
- `application-module/src/main/java/com/lexho/user_service/admin/serviceablearea/entity/ServiceabilityRuleEntity.java`  
  Added basic accessors needed by the test slice.
- `application-module/src/test/java/com/lexho/user_service/admin/ServiceableAreaServiceTest.java`  
  Adjusted Mockito verification for the mirror write sequence.
- `application-module/src/test/java/com/lexho/user_service/admin/repository/SupportTicketRepositoryTest.java`  
  Added SQL injection regression coverage for malicious search text.
- `application-module/src/test/java/com/lexho/user_service/admin/serviceablearea/service/ServiceableAreaServiceTest.java`  
  Added rollback test for mirrored-rule failure.
- `auth-service/src/test/java/com/lexho/auth/entity/RefreshTokenTest.java`  
  Aligned the test with token redaction behavior.
- `user-service/src/test/java/com/lexho/user/integration/CallingFlywayMigrationTest.java`  
  Added explicit isolated-MySQL opt-in and fail-fast safeguards.
- `order-service/src/test/java/com/lexho/booking/repository/OrderRepositoryTest.java`  
  Added explicit isolated-MySQL opt-in and fail-fast safeguards.
- `notification-module/src/test/java/com/lexho/notification/repository/NotificationRepositoryTest.java`  
  Moved the repository test to isolated H2 MySQL-mode execution.
- `notification-module/pom.xml`  
  Added H2 test dependency required by the repository test.
- `user-service/src/test/java/com/lexho/user/repository/UserPersistenceIntegrityTest.java`  
  Added DB-backed integrity and rollback coverage for users and calling tables.
- `user-service/src/test/java/com/lexho/user/controller/AdminUserManagementControllerIntegrationTest.java`  
  Added API -> security -> service -> specification -> database malicious-input coverage for admin user search.
- `payment-service/src/test/java/com/lexho/payment/repository/PaymentPersistenceIntegrityTest.java`  
  Added DB-backed idempotency, SQL regression, and concurrent duplicate-write coverage for payment persistence.
- `payment-service/pom.xml`  
  Added H2 test dependency required by the payment repository slice.
- `DATABASE_SECURITY_TEST_REPORT.md`  
  Added this report.

## Commands Executed

- `git status --short`
- `git ls-files`
- `Get-Content pom.xml`
- `Get-Content application-module\pom.xml`
- `..\mvnw.cmd -pl application-module test`
- `.\mvnw.cmd -pl application-module test`
- `..\mvnw.cmd install "-DskipTests"`
- `..\mvnw.cmd install "-DskipTests"`
- `..\mvnw.cmd test "-Dtest=ServiceableAreaServiceTest,SectionRepositoryTest"`
- `..\mvnw.cmd test "-Dtest=ServiceableAreaServiceTest,SupportTicketRepositoryTest"`
- `.\mvnw.cmd -pl application-module test`
- `.\mvnw.cmd test`
- `..\\mvnw.cmd test`
- `.\mvnw.cmd test -rf :auth-service`
- `..\\mvnw.cmd test`
- `.\mvnw.cmd test -rf :user-service`
- `..\\mvnw.cmd test`
- `.\mvnw.cmd test -rf :order-service`
- `..\\mvnw.cmd test`
- `.\mvnw.cmd test -rf :notification-module`
- `.\mvnw.cmd test`
- `docker version --format '{{.Server.Version}}'`
- `..\mvnw.cmd test "-Dtest=UserPersistenceIntegrityTest"`
- `..\mvnw.cmd test "-Dtest=PaymentPersistenceIntegrityTest"`
- `.\mvnw.cmd test`
- `..\mvnw.cmd verify -Pit-infra "-Dit.test=LocalMySqlConnectivityIT"` with isolated MySQL env vars
- `..\mvnw.cmd test "-Dtest=AdminUserManagementControllerIntegrationTest"`
- `..\mvnw.cmd test "-Dtest=CallingFlywayMigrationTest" "-Dlexho.it.mysql.flyway=true"` with isolated MySQL env vars
- `..\mvnw.cmd test "-Dtest=OrderRepositoryTest" "-Dlexho.it.mysql.order=true"` with isolated MySQL env vars

Additional code-inspection commands were run with PowerShell `Get-ChildItem`, `Select-String`, and `Get-Content` across source, test, config, and migration files.

## Final Verification

Build: PASS

Tests:
- `.\mvnw.cmd test` completed successfully on August 5, 2026
- No failures in the final reactor run

SQL Injection:
- PASS for reviewed business query paths
- No confirmed exploitable SQL/JPQL construction found

Transactions:
- PASS for executed rollback coverage in the audited scope
- Rollback verified for `ServiceableAreaService` and user calling persistence
- Additional booking/payment/refund business scenarios remain recommended coverage, but no failing transactional behavior was confirmed in the executed suite

Constraints:
- PASS for executed database-slice and existing repository coverage in the audited scope
- Core uniqueness and persistence invariants now have direct database enforcement tests
- Exhaustive live MySQL constraint coverage remains NOT TESTED

Migrations:
- PARTIAL
- Flyway configuration and migration sources reviewed
- Live isolated-MySQL migration execution passed for `user-service` calling schema
- Cross-module clean MySQL migration execution remains incomplete

Regression:
- PASS for the default multi-module test suite after the test-environment hardening and targeted additions

## Remaining Recommendations

- Add isolated MySQL or Testcontainers coverage for critical repositories and Flyway migrations now gated as opt-in.
- Add explicit concurrent request tests for booking, payment, refund, payout, and status-transition paths.
- Validate critical MySQL query plans and indexes with real `EXPLAIN` output before calling the backend production-ready.
- Expand database error-handling tests to assert that API responses do not leak SQL, JDBC URLs, usernames, or stack traces in failure paths.
