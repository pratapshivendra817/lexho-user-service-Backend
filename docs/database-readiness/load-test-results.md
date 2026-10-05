# Load Test Results

Status date: August 15, 2026

## Health/Readiness Load Executed

- k6 runner:
  - `docker run --rm -v "C:\Users\shivendra\Downloads\lexho-user-service-master:/work" -w /work grafana/k6 run scripts/db-load-validation.js -e BASE_URL=http://host.docker.internal:8082`
- application target:
  - `application-module` running on `http://127.0.0.1:8082`
- database target:
  - MySQL 8.0 container on `127.0.0.1:3307`

## Result

- `PASS`
- total requests: `599`
- `http_req_failed=0.00%`
- `p95=17.44ms`
- `p99=21.66ms`

## Notes

- The script was aligned to the app's custom health endpoints:
  - `/health/liveness`
  - `/health/readiness`
- Readiness `503` is treated as expected for this isolated environment because Redis and payment-gateway credentials were intentionally absent during the run.

## Business-Flow DB Load Executed

- Maven runner:
  - `.\mvnw.cmd -B -ntp -pl application-module "-Dlexho.it.mysql.concurrency=true" "-Dtest=ApplicationMysqlConcurrencyIT" test`
- application schema target:
  - isolated MySQL database `lexho_application_concurrency_test`
- workload:
  - 8 worker threads
  - 120 total booking/payment/dispatch-request/payout-style backend DB flows
  - Flyway clean migration to version `v118` before execution

## Business-Flow DB Load Result

- `PASS`
- tests run: `3`
- failures: `0`
- errors: `0`
- skipped: `0`
- business-flow failures: `0`
- asserted latency thresholds:
  - `p95 < 1000ms`
  - `p99 < 2500ms`

## Scope

- Health/readiness HTTP load is covered by `scripts/db-load-validation.js`.
- Production-like backend business-flow DB load is covered by `ApplicationMysqlConcurrencyIT`.
- Full frontend/device and external-gateway traffic profiling remains a staging or production-observability activity.
