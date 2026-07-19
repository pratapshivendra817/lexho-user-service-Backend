# Cleanup Report

Date: 2026-05-27
Repository: `lexho-user-service-master`

## Scope Applied

Safe cleanup only:
- build/cache artifacts
- IDE metadata
- runtime logs
- temporary payload/debug files
- unneeded local archive
- untracked runtime-generated uploads

No source/business/runtime config/database migration/API contract files were deleted.

## Deleted Items

### Directories Deleted (12)
- `.idea/`
- root `target/`
- module `target/` directories under:
  - `admin-module/`
  - `application-module/`
  - `auth-service/`
  - `catalog-module/`
  - `common-lib/`
  - `notification-module/`
  - `order-service/`
  - `partner-module/`
  - `payment-service/`
  - `user-service/`

### Files Deleted (67)
Deleted patterns:
- `*.iml`
- `*.log`, `*.out.log`, `*.err.log`
- `resp_*.json`, `b_*.json`, `t_*.json`
- `*_body.txt`
- `temp_*`, `debug_*`
- `*.zip` (deleted: `apache-maven-3.9.6-bin.zip`)

### Untracked Upload Assets Deleted (13)
Deleted untracked runtime files from:
- `uploads/services/*.jpeg`

## Skipped Items (Intentionally Preserved)

- All `src/main/**`, `src/test/**`, `pom.xml`, `mvnw`, `mvnw.cmd`
- runtime configuration and migrations (`application*.properties/yml`, `db/migration/**`)
- Docker/Jenkins/K8s and contract/doc files
- `.git/**` internals (including `.git/logs`)
- tracked upload assets under `uploads/`

Tracked uploads preserved:
- Count: 165 files
- Size: 49,146,330 bytes

## Risky / Manual-Verify Items

1. `uploads/**` (tracked assets)
   - Could be fixture/demo or expected local media for manual testing.
   - Not deleted automatically.
2. Any deleted tracked log/payload files
   - Currently shown as staged deletions in git status.
   - Keep deleted if desired, or restore selectively with git.

## Large Files (>20MB)

- None detected after cleanup scan.

## Estimated Space Saved

- Approx. `37,947,652` bytes (~36.2 MB) removed.

## .gitignore Improvements Applied

Added/ensured:
- `target/`, `build/`, `out/`
- `logs/`, `*.log`, `*.out.log`, `*.err.log`
- `.idea/`, `*.iml`
- `uploads/`
- `resp_*.json`, `b_*.json`, `t_*.json`, `*_body.txt`, `temp_*`, `debug_*`
