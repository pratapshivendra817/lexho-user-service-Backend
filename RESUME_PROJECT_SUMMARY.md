# Lexho Backend - Resume Project Summary

## Short Resume Version

**Lexho Service Marketplace Backend**  
Built a production-oriented, multi-module Spring Boot backend for an on-demand home services marketplace covering customer, partner, admin, booking, payment, notification, dispatch, live tracking, catalog, review, support, and subscription workflows. Implemented secure REST APIs, JWT-based authentication, role-based access control, Razorpay payment/webhook handling, real-time partner tracking, Firebase push notifications, Redis-backed geo/dispatch flows, Flyway database migrations, observability with Actuator/Prometheus, and automated test coverage across core modules.

## Resume Bullet Points

- Developed a Java 17 / Spring Boot 3 multi-module backend for a home-service marketplace with customer, partner, admin, order, payment, catalog, notification, and application runtime modules.
- Designed and implemented REST APIs for authentication, user management, service catalog, cart, bookings, partner runtime operations, admin operations, reviews, support, profile management, and serviceable-area management.
- Integrated Razorpay payments with secure webhook signature verification, idempotent event processing, retry metadata, and payment/refund lifecycle tracking.
- Built real-time dispatch and partner-location capabilities using Redis GEO concepts, geohash-based partitioning hooks, WebSocket/SSE-style event flows, and scalable tracking lifecycle services.
- Added Firebase Cloud Messaging support for push notifications and notification orchestration for customer, partner, and admin workflows.
- Implemented production-readiness features including global error contracts, request/correlation IDs, audit logging, Spring Boot Actuator health/metrics endpoints, Prometheus metrics, and structured request tracing.
- Managed relational persistence with Spring Data JPA, MySQL, Flyway migrations, DTO validation, MapStruct/Lombok, and layered controller-service-repository architecture.
- Strengthened backend quality using JUnit, Spring Boot Test, Spring Security Test, H2/Testcontainers, JaCoCo coverage reporting, Checkstyle, and SpotBugs.

## Detailed Project Description

Lexho is a backend system for an on-demand home services platform where customers can browse services, manage cart and addresses, place bookings, make payments, track service partners, submit reviews, and receive notifications. Partners can manage profiles, orders, live location, earnings, withdrawals, service areas, leaves, and operational status. Admin users can manage catalog, bookings, customers, partners, payments, payouts, banners, serviceable areas, app settings, support content, emergency workflows, and monitoring.

The project follows a Maven multi-module structure with a single Spring Boot runtime module that composes domain modules such as `auth-service`, `user-service`, `order-service`, `payment-service`, `partner-module`, `catalog-module`, `notification-module`, `admin-module`, `common-lib`, and `application-module`.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.5.x
- **Build:** Maven multi-module
- **Database:** MySQL, H2 for tests
- **Persistence:** Spring Data JPA, Hibernate, Flyway
- **Security:** Spring Security, JWT
- **Realtime / Async:** WebSocket, RabbitMQ, Redis
- **Payments:** Razorpay Java SDK and webhooks
- **Notifications:** Firebase Admin SDK, email support
- **API Docs:** Springdoc OpenAPI / Swagger
- **Observability:** Spring Boot Actuator, Micrometer, Prometheus
- **Resilience:** Resilience4j
- **Testing / Quality:** JUnit, Spring Boot Test, Testcontainers, Spring Security Test, JaCoCo, Checkstyle, SpotBugs

## Major Modules / Features

- **Authentication and Security:** JWT filters, role-based access, admin auth properties, protected APIs, rate limiting, and global exception handling.
- **Customer App Backend:** service browsing, cart, bookings, address/location management, profile, reviews, support, notifications, subscriptions, and account governance.
- **Partner App Backend:** partner runtime governance, service areas, orders, earnings, withdrawals, leaves, device tokens, notifications, and live tracking.
- **Admin Backend:** booking management, partner/customer governance, catalog/category/service management, banners, sections, payouts, settings, support FAQs, serviceable areas, emergency and monitoring workflows.
- **Booking and Dispatch:** order lifecycle, partner matching, partner skills, location updates, nearby partner search, dispatch scoring, cleanup schedulers, and tracking lifecycle management.
- **Payments:** Razorpay order/payment integration, webhook processing, retry-safe event storage, refunds, payment status tracking, and payout-related workflows.
- **Notifications:** Firebase push notifications, notification events/listeners, admin bulk notifications, recipient resolution, and async notification handling.
- **Production Readiness:** audit logs, correlation IDs, request tracing, health checks, Prometheus metrics, OpenAPI contracts, scaling playbook, and backend readiness documentation.

## Interview Talking Points

- Explain how the system is split into Maven modules but packaged through one Spring Boot runtime module.
- Discuss how Razorpay webhooks were made safe using signature verification, event IDs, idempotency, and retry/dead-letter style status tracking.
- Describe how live partner tracking and nearby partner search can scale using Redis GEO/geohash partitioning and separate ingest/read/dispatch responsibilities.
- Highlight production engineering work: API error contract, audit logging, request IDs, Prometheus metrics, health endpoints, and readiness documents.
- Mention test strategy: unit tests for services, controller tests for API behavior, security tests, H2/Testcontainers for persistence, and JaCoCo coverage reports.

## One-Line Resume Entry

**Lexho Backend:** Developed a production-ready Java 17/Spring Boot multi-module backend for an on-demand services marketplace, implementing secure APIs, bookings, payments, partner dispatch/live tracking, notifications, admin operations, observability, Flyway migrations, and automated test coverage.
