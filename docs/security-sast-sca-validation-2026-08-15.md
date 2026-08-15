# Security SAST/SCA Validation - August 15, 2026

## Current Status

Production backend local SAST gate for Customer, Partner, and Admin app code paths is `PASS` for the repository-provable static-analysis scope.

Formal hosted SAST/SCA certification is now wired in CI through `.github/workflows/security-sast-sca.yml` and must be run in GitHub Actions for final external evidence.

## Local Evidence

Executed from the repository root on August 15, 2026:

```powershell
.\mvnw.cmd -B -ntp verify -DskipTests
```

Result: `PASS`

Covered local static-analysis gates:

- Maven Checkstyle
- SpotBugs with `effort=Max`
- all backend modules in the reactor:
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

Observed result:

- Checkstyle violations: `0`
- SpotBugs bug instances: `0`
- Reactor result: `BUILD SUCCESS`

## Existing Security Regression Evidence

Latest backend security regression evidence remains:

- `target/security-regression/security-production-regression-20260815-140230.json`
- result: `PASS`
- checks: `22`
- failed checks: `0`

Covered:

- Customer, Partner, and Admin role-boundary checks
- missing, expired, tampered, and wrong-signature JWT rejection
- XSS, SQL injection, path traversal, IDOR, and mass-assignment probes
- tracked source/config secret scan

## CI Security Workflow Added

Added workflow:

- `.github/workflows/security-sast-sca.yml`

The workflow runs:

- Maven Checkstyle and SpotBugs
- CodeQL Java/Kotlin analysis
- Semgrep Java and OWASP Top 10 rules
- GitHub Dependency Review on pull requests
- OWASP Dependency-Check with `failBuildOnCVSS=7`
- Trivy filesystem scan for high/critical findings

Artifacts uploaded by the workflow:

- Checkstyle XML reports
- SpotBugs XML reports
- Semgrep SARIF
- OWASP Dependency-Check HTML/JSON reports
- Trivy SARIF

## GitHub Actions Run Notes

Manual workflow dispatch was triggered after adding the workflow.

Observed CI status:

- `PASS`: Semgrep SAST completed successfully and uploaded SARIF/artifacts.
- `BLOCKED`: Maven Checkstyle/SpotBugs and CodeQL build jobs failed because the remote branch did not yet contain all source files present in the local workspace. The failed remote compile reported missing application classes including `DispatchProperties`, partner warning comment DTO/entity/repository/service classes, activity feed services, and notification realtime service.
- `BLOCKED`: Trivy initially reached the scanner but failed when Maven Central returned `429 Too Many Requests` while resolving Maven POM metadata. The workflow was updated to preload the Maven dependency cache and mount it into the Trivy container.
- `BLOCKED`: OWASP Dependency-Check exceeded the practical run window during CVE database initialization/update and was cancelled after the overall run had already failed.

Next requirement for final hosted evidence: commit and push the pending backend source changes that make the remote branch match the locally buildable workspace, then rerun `Security SAST and SCA`.

## Local SCA Note

OWASP Dependency-Check was attempted locally with:

```powershell
.\mvnw.cmd -B -ntp org.owasp:dependency-check-maven:12.1.3:check -DskipTests -Dformat=HTML -Dformat=JSON -DfailBuildOnCVSS=7
```

It did not complete within the local execution window because dependency vulnerability database initialization/update exceeded the command timeout. No Dependency-Check report was produced locally.

## Release Meaning

Customer, Partner, and Admin backend code now has a passing local SAST/static-analysis gate.

Do not claim final formal SAST/SCA production attestation until the new GitHub Actions security workflow completes successfully and its CodeQL, Semgrep, Dependency-Check, and Trivy artifacts are retained.
