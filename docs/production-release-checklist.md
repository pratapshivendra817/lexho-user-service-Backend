# Production Release Checklist

Use this checklist before promoting the backend to production.

## Required Secrets

Set these in the deployment secret manager or host environment. Do not commit them.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` with at least 32 characters and a unique production value
- `INTERNAL_API_TOKEN` with at least 24 characters
- `APP_ALLOWED_CORS_ORIGINS` with only production frontend origins
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `APP_PUSH_FCM_ENABLED=true` when push is enabled
- `APP_PUSH_FCM_SERVICE_ACCOUNT_BASE64` or `APP_PUSH_FCM_SERVICE_ACCOUNT_PATH`

If admin seed is needed for a fresh environment:

- `ADMIN_SEED_ENABLED=true`
- `ADMIN_SEED_EMAIL`
- `ADMIN_SEED_PASSWORD` with a unique production password

Disable admin seed after the first production admin exists.

## Secret Rotation

Rotate any secret that was ever present in a committed or shared file:

- Database password
- JWT signing secret
- Internal API token
- Firebase service account key
- Razorpay key secret
- Razorpay webhook secret
- Admin seed password

After rotation, invalidate old keys in the provider dashboards and redeploy with only the new values.

## Pre-Deploy Gates

Run from the repository root:

```powershell
.\mvnw.cmd -pl application-module -am verify
.\mvnw.cmd -pl coverage-report verify
```

Optional strict static-analysis audit:

```powershell
.\mvnw.cmd -pl application-module -DskipTests "-Dspotbugs.skip=false" spotbugs:check
```

The explicit application-module SpotBugs gate currently reports legacy medium-priority findings. Do not enable it as a blocking gate until those are triaged or suppressed with rationale.

## Staging Smoke

Deploy with:

- `SPRING_PROFILES_ACTIVE=prod`
- Real staging MySQL
- Real Redis if Redis-backed OTP/rate limiting is required
- Staging Firebase credentials if push is enabled
- Razorpay test-mode keys in staging

Verify:

- `GET /health/liveness` returns `200`
- `GET /health/readiness` returns `200`
- Admin login works with seeded or existing admin
- Admin warning create persists a warning
- `GET /api/v1/partner/warnings` returns the warning for that partner
- Partner notification list includes warning notification when notification sending is enabled
- Push payload has `redirectType=WARNING`, `warningId`, and `redirectValue`
- `PATCH /api/v1/partner/notifications/{notificationId}/read` updates unread count

Warning-specific smoke can be run with existing admin and partner JWTs:

```powershell
.\scripts\staging-warning-smoke.ps1 `
  -BaseUrl "https://staging-api.example.com" `
  -AdminToken "<admin-jwt>" `
  -PartnerToken "<partner-jwt>" `
  -PartnerId 123
```

## Flyway

Before production migration, validate against a copy of production data:

```powershell
.\mvnw.cmd -pl application-module -DskipTests spring-boot:run -Dspring-boot.run.profiles=prod
```

Confirm Flyway validates the configured locations:

- `classpath:db/migration/application-module`
- `classpath:db/migration/user-service`

If production has history from the old flat migration path, compare `flyway_schema_history` checksums before migration.

## Opt-in MySQL Proofs

Run these against isolated MySQL databases before a production claim:

```powershell
.\mvnw.cmd -pl application-module "-Dlexho.it.mysql.application.flyway=true" "-DIT_DB_URL=jdbc:mysql://localhost:3306/lexho_application_flyway_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" "-DIT_DB_USERNAME=<user>" "-DIT_DB_PASSWORD=<password>" "-Dtest=ApplicationMysqlFlywayMigrationIT" test

.\mvnw.cmd -pl application-module "-Dlexho.it.mysql.explain=true" "-DIT_DB_URL=jdbc:mysql://localhost:3306/lexho_application_explain_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" "-DIT_DB_USERNAME=<user>" "-DIT_DB_PASSWORD=<password>" "-Dtest=ApplicationMysqlExplainPlanIT" test
```

These runs are expected to prove:

- clean MySQL migration execution
- critical unique/index contracts exist after migration
- critical query paths resolve to the expected MySQL indexes

## Backup And Restore Rehearsal

Do not call the database layer production-ready without a recorded restore drill.

Use:

- [db-backup-restore-rehearsal.md](/C:/Users/shivendra/Downloads/lexho-user-service-master/docs/db-backup-restore-rehearsal.md)
