# User Management Module (user-module)

## Folder Structure

```text
user-module
└── src
    ├── main
    │   └── java/com/lexho/user
    │       ├── client
    │       │   └── BookingServiceClient.java
    │       ├── controller
    │       │   └── AdminUserManagementController.java
    │       ├── dto
    │       │   ├── AdminUserCreateRequest.java
    │       │   ├── AdminUserUpdateRequest.java
    │       │   ├── AdminUserResponse.java
    │       │   ├── AdminUserDetailResponse.java
    │       │   ├── BookingSummaryDto.java
    │       │   ├── BookingSummaryClientResponse.java
    │       │   └── PageResponse.java
    │       ├── entity
    │       │   └── User.java
    │       ├── enums
    │       │   └── UserStatus.java
    │       ├── exception
    │       │   ├── UserAlreadyExistsException.java
    │       │   ├── UserNotFoundException.java
    │       │   └── InvalidRoleException.java
    │       ├── mapper
    │       │   └── UserManagementMapper.java
    │       ├── repository
    │       │   └── UserRepository.java
    │       └── service
    │           └── UserManagementService.java
    └── test
        └── java/com/lexho/user/service
            └── UserManagementServiceTest.java
```

## MySQL Schema (managed via Flyway)

Migration file:
- `application-module/src/main/resources/db/migration/V4__user_management_upgrade.sql`

Key columns on `customers` table:
- `id`, `full_name`, `email`, `phone_number`, `password`, `role`
- `profile_image`, `city`, `state`, `address`
- `status`, `total_bookings`, `deleted`
- `created_at`, `updated_at`

Indexes:
- `idx_customers_phone_number`
- `idx_customers_email`
- `idx_customers_role`
- `idx_customers_status`

## Admin User Management APIs

Base path: `/api/admin/users`

1. `GET /api/admin/users?page=0&size=10&role=PARTNER&status=ACTIVE`
2. `POST /api/admin/users`
3. `PUT /api/admin/users/{id}`
4. `PUT /api/admin/users/{id}/block`
5. `PUT /api/admin/users/{id}/unblock`
6. `GET /api/admin/users/{id}`
7. `DELETE /api/admin/users/{id}`

## Sample Requests

### Add User

```json
{
  "fullName": "Amit Sharma",
  "email": "amit@lexho.com",
  "phoneNumber": "9876543210",
  "password": "Admin@12345",
  "role": "PARTNER",
  "profileImage": "http://localhost:8082/uploads/services/abc.jpg",
  "city": "Bhopal",
  "state": "MP",
  "address": "Vijay Nagar"
}
```

### Update User

```json
{
  "fullName": "Amit K Sharma",
  "phoneNumber": "9876543210",
  "address": "Arera Colony",
  "role": "CUSTOMER",
  "city": "Bhopal",
  "state": "MP",
  "profileImage": "http://localhost:8082/uploads/services/new.jpg"
}
```

## Sample Response

```json
{
  "id": 1,
  "fullName": "Amit Sharma",
  "email": "amit@lexho.com",
  "phoneNumber": "9876543210",
  "role": "PARTNER",
  "profileImage": "http://localhost:8082/uploads/services/abc.jpg",
  "city": "Bhopal",
  "state": "MP",
  "address": "Vijay Nagar",
  "status": "ACTIVE",
  "totalBookings": 5,
  "createdAt": "2026-04-26T12:00:00",
  "updatedAt": "2026-04-26T12:00:00"
}
```

## Feign Integration

- Feign client: `BookingServiceClient`
- Target endpoint: `GET /internal/bookings/users/{userId}/summary`
- Backing partner endpoint implemented in `InternalBookingController`
