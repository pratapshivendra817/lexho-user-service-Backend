# Local To Production Roadmap

This document is the practical deployment path for this backend when developed by a single engineer.

Scope:

- local development
- local testing
- production hardening
- AWS deployment
- no-Docker first deployment
- when to adopt Docker later

## Recommended Approach

Build and stabilize the backend locally first.

Do not take AWS infrastructure early unless one of these is already true:

- core booking and payment flows are stable
- schema changes are slowing down because of environment drift
- external testing needs a public environment
- deployment rehearsal is the next blocker

Recommended order:

1. Complete features locally
2. Complete developer testing locally
3. Fix security and correctness issues
4. Freeze API behavior
5. Verify migrations and production config
6. Create AWS infrastructure
7. Deploy to production-like environment
8. Run final production-readiness verification

## Phase 1: Local Development

Use local development for:

- controller and service implementation
- DTO validation
- auth flow
- booking flow
- payment flow
- Flyway migration authoring
- repository behavior
- exception handling
- logging behavior

Local expectations:

- Java version must match the project runtime version
- local MySQL should be available, or Testcontainers should be used where possible
- secrets must come from `.env`, environment variables, or profile config, not hardcoded code paths

Do not treat local success as production readiness.

## Phase 2: Local Testing

Complete these locally before buying production infrastructure:

- unit tests
- controller tests
- repository tests
- authentication tests
- authorization tests
- validation tests
- SQL injection regression tests
- payment idempotency tests
- booking concurrency tests

Minimum local verification checklist:

- application starts cleanly
- Flyway migrations apply on a clean database
- key APIs work end-to-end
- invalid input returns consistent errors
- unauthorized requests are rejected
- duplicate and replay-prone operations are handled safely

## Phase 3: Production Hardening Before AWS

Finish these before deployment:

- remove committed secrets
- separate dev, staging, and production config
- disable unsafe debug and fallback behavior
- verify JWT and OTP security
- verify rate limits on sensitive endpoints
- verify consistent exception responses
- verify logs do not leak secrets, OTPs, tokens, or payment secrets
- verify `ddl-auto` is not doing unsafe schema mutation in production
- verify Actuator exposure is minimal

Production profile should explicitly define:

- datasource URL
- datasource username
- datasource password from environment
- Hikari pool settings
- Flyway enabled
- Hibernate validate mode only
- CORS allowlist
- logging levels
- external API timeouts

## When To Buy RDS

Take RDS when you are close to deployment, not at the start of coding.

RDS becomes justified when:

- schema is reasonably stable
- local flows are working
- deployment rehearsal is needed
- you need production-like database behavior
- backups and managed operations start mattering more than local speed

Do not delay RDS until after public launch.

Best point to provision RDS:

- after local backend stabilization
- before final production validation
- before real customer traffic

## Recommended First Production Architecture

For this project, a practical first production setup is:

- EC2 for the Spring Boot application
- RDS MySQL for the database
- S3 for uploads or generated files
- Nginx as reverse proxy
- Let's Encrypt for TLS
- `systemd` for app process management

No Docker is required for the first production deployment.

## No-Docker First Deployment

This is the recommended starting path for a single engineer.

Advantages:

- simpler setup
- easier debugging
- less deployment overhead
- fewer moving parts

Tradeoffs:

- environment consistency is weaker than containers
- rollback is more manual
- scaling to multiple identical app nodes takes more discipline

Use this setup:

- `EC2 t4g.small`
- `RDS MySQL db.t4g.small` Single-AZ
- `S3`
- `Nginx`
- `systemd`

## When Docker Becomes Worth It

Adopt Docker later if one or more of these become true:

- deployments become frequent
- multiple environments drift from each other
- rollback needs to be faster
- CI/CD pipeline needs consistent artifacts
- more engineers start deploying
- multiple app instances are required

Docker is an operational improvement, not a prerequisite for a valid production launch.

## Pre-Deployment Checklist

Before first AWS deployment:

- all critical business flows tested locally
- production secrets moved out of source control
- production profile reviewed
- Flyway migration path verified
- auth and authorization reviewed
- payment webhook verification tested
- backups planned
- health endpoints reviewed
- logs reviewed for sensitive data leakage
- rate limits applied to auth and abuse-prone endpoints

## Deployment Checklist

At deployment time:

1. Provision RDS MySQL
2. Provision EC2
3. Provision S3 bucket if uploads are needed
4. Configure security groups
5. Install Java on EC2
6. Copy the application jar
7. Configure environment variables
8. Run Flyway-backed startup against RDS
9. Put Nginx in front of the app
10. Install TLS certificate
11. Register `systemd` service
12. Verify health endpoint
13. Verify login flow
14. Verify one protected API
15. Verify logging and restart behavior

## Post-Deployment Verification

After deployment, verify:

- application starts after reboot
- database connectivity is stable
- request logs contain request IDs
- health endpoint is reachable
- admin APIs are not publicly exposed
- OTPs and tokens are not logged
- payment callbacks are authentic and idempotent
- backups exist
- restore procedure is documented

## Final Recommendation

For this project and current ownership model:

- develop locally first
- harden locally first
- buy RDS near deployment time
- deploy first production version without Docker
- adopt Docker later when operations justify it

That sequence is the lowest-friction path without compromising production quality.
