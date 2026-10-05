# Lexho Partner Backend

## Layered Structure
- `controller`: `com.lexho.user_service.lexhopartner.controller`
- `service`: `com.lexho.user_service.lexhopartner.service`
- `repository`: `com.lexho.user_service.lexhopartner.repository`
- `entity`: `com.lexho.user_service.lexhopartner.entity`
- `dto`: `com.lexho.user_service.lexhopartner.dto`
- `util`: `com.lexho.user_service.lexhopartner.util`

Existing shared modules already provide:
- `security`: JWT + filter + stateless session (`config/SecurityConfig`, `config/JwtFilter`)
- `config`: OpenAPI, CORS, env guards
- `exception`: global exception handling (`exception/GlobalExceptionHandler`)

## APIs
- `PUT /api/partner/availability`
- `GET /api/orders/new`
- `POST /api/orders/{id}/accept`
- `POST /api/orders/{id}/reject`
- `POST /api/orders/{id}/start`
- `POST /api/orders/{id}/complete`
- `GET /api/orders/history`
- `GET /api/orders/dashboard`
- `GET /api/notifications`
- `GET /api/health`

## DB Schema
Flyway migration: `V29__lexho_partner_marketplace_core.sql`
- `partners`
- `orders` (extended for partner workflow)
- `order_status_history`
- `partner_availability`
- `refresh_tokens` (already present in auth module)

## Sample Response
```json
{
  "success": true,
  "message": "Profile fetched",
  "data": {
    "id": 101,
    "name": "Rahul",
    "mobileNumber": "9876543210",
    "email": "rahul@lexho.com",
    "city": "Delhi",
    "address": "Dwarka",
    "serviceCategory": "cleaning",
    "rating": 4.8,
    "profileImage": "/uploads/partner-profile/abc.jpg",
    "online": true,
    "verified": false,
    "createdAt": "2026-05-12T17:12:00"
  }
}
```
