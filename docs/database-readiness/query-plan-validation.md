# Query Plan Validation

Status date: August 5, 2026

## Implemented

- Real MySQL EXPLAIN assertions:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlExplainPlanIT.java`

Queries covered:

- `orders` by `assigned_partner_id`
- `payment_transactions` by `booking_id, created_at`
- `payment_webhook_events` by `status, next_retry_at`
- `call_session` by `caller_id, status`

## Executed

- `NOT EXECUTED — ENVIRONMENT REQUIRED`
  - Docker daemon was unavailable in this session.

Exact command:

```powershell
.\mvnw.cmd -pl application-module "-Dlexho.it.mysql.explain=true" "-DIT_DB_URL=jdbc:mysql://127.0.0.1:3308/lexho_application_explain_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" "-DIT_DB_USERNAME=root" "-DIT_DB_PASSWORD=<password>" "-Dtest=ApplicationMysqlExplainPlanIT" test
```

## PASS Criteria

- Expected index appears in MySQL `EXPLAIN`
- No unexpected full scan on covered hot-path query
- Rows examined stay aligned with index access expectations
