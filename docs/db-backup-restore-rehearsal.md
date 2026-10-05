# DB Backup And Restore Rehearsal

Use this runbook before calling the database layer production-ready.

Status: this document defines the required rehearsal. It is not evidence by itself.

## Goal

Prove that:

1. backups can be created on schedule
2. a recent snapshot can be restored into an isolated database
3. Flyway validation still passes on the restored data
4. the application can boot against the restored database in `prod`

## Minimum Inputs

- production-like MySQL or RDS snapshot source
- isolated restore target database
- production-equivalent `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_API_TOKEN`
- `APP_ALLOWED_CORS_ORIGINS`

## Evidence To Capture

- backup or snapshot identifier
- snapshot creation timestamp
- restore target database name
- restore completion timestamp
- Flyway validation result
- application startup result
- one successful health probe
- owner and reviewer names

## Rehearsal Steps

1. Create a fresh backup or snapshot of the staging or production-clone database.
2. Restore that backup into a new isolated database.
3. Point the app at the restored database with production profile secrets.
4. Start the application:

```powershell
.\mvnw.cmd -pl application-module -DskipTests spring-boot:run -Dspring-boot.run.profiles=prod
```

5. Confirm startup completes without Flyway checksum or schema-validation failure.
6. Verify:
   - `GET /health/liveness`
   - `GET /health/readiness`
7. Record whether any manual repair was required.
8. Destroy the temporary restored database after evidence is captured.

## Failure Criteria

Treat the rehearsal as failed if any of the following happens:

- backup creation fails
- restore does not complete
- Flyway validation fails
- Hibernate `validate` fails
- app startup requires schema edits outside migrations
- health probes fail after startup

## Evidence Template

```text
Date:
Operator:
Reviewer:
Source snapshot ID:
Snapshot created at:
Restore target:
Restore completed at:
Flyway result:
Application startup result:
Health check result:
Notes:
Follow-up actions:
```
