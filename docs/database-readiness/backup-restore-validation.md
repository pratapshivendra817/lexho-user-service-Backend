# Backup Restore Validation

Status date: August 15, 2026

## Executed

- source database:
  - `lexho_application_production_readiness_test`
- restore database:
  - `lexho_backup_restore_restored`
- method:
  - `mysqldump` from isolated MySQL 8.0 on `127.0.0.1:3307`
  - restore piped back into a fresh database on the same isolated MySQL instance

## Result

- `PASS`
- representative order row inserted into source database
- backup dump created successfully
- restore database rebuilt successfully
- row counts matched:
  - source orders: `1`
  - restored orders: `1`
- duration: `134` seconds in the August 15 rerun

## Notes

- Backup file was written to a temporary local path during execution.
- This validation proved create-dump, rebuild, and row-parity behavior for the isolated production-readiness schema.
