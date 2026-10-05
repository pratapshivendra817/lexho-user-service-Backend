# Migration Validation

Status date: August 5, 2026

## Implemented

- Fresh isolated-MySQL full migration test:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlProductionReadinessIT.java`
- Schema contract migration test:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlFlywayMigrationIT.java`
- Upgrade migration test:
  - `application-module/src/test/java/com/lexho/user_service/integration/ApplicationMysqlUpgradeMigrationIT.java`
- Calling-schema migration test:
  - `user-service/src/test/java/com/lexho/user/integration/CallingFlywayMigrationTest.java`

## Executed

- `PASS` static migration coverage:
  - `.\mvnw.cmd -pl application-module "-Dtest=ApplicationModuleFlywayMigrationTest" test`

## Not Executed

- `NOT EXECUTED — ENVIRONMENT REQUIRED`
  - Docker daemon was unavailable in this session.
  - Exact command:

```powershell
.\scripts\run-mysql-validation-suite.ps1
```

## PASS Criteria

- Fresh MySQL database created
- All Flyway migrations applied from zero
- No failed or pending required migrations
- Critical tables, keys, constraints, indexes, defaults verified
- Application context starts against migrated schema
