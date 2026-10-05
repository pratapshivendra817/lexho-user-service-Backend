# Partner and Admin Support API

This file documents the backend support APIs for partner app and admin app integration.

## Partner App APIs

Base paths:

- `/api/v1/partner/support`
- `/api/partner/support`

All partner endpoints require partner/admin authentication and use the authenticated `userId` as `partnerId`.

### Create Partner Support Ticket

```http
POST /api/v1/partner/support/tickets
Content-Type: application/json
Authorization: Bearer <token>
```

Payload:

```json
{
  "subject": "Payment issue",
  "message": "Payout delayed",
  "categoryId": "PAYOUT",
  "bookingId": "BK-1",
  "priority": "HIGH"
}
```

Rules:

- `subject` is required, max 200 chars.
- `message` is required, max 2000 chars.
- `priority` must be one of `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- If `priority` is omitted, backend stores `MEDIUM`.
- If `categoryId` is omitted, backend stores partner category as `GENERAL`.

Response:

```json
{
  "success": true,
  "message": "Ticket created",
  "data": {
    "id": "TKT-000012",
    "subject": "Payment issue",
    "message": "Payout delayed",
    "status": "OPEN",
    "categoryId": "PAYOUT",
    "bookingId": "BK-1",
    "priority": "HIGH",
    "createdAt": "2026-07-20T17:40:00",
    "updatedAt": "2026-07-20T17:40:00"
  }
}
```

### Partner Ticket History

```http
GET /api/v1/partner/support/tickets?status=OPEN&page=0&size=20
Authorization: Bearer <token>
```

Query params:

- `status` optional: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `page` default `0`
- `size` default `20`, max `100`

Response:

```json
{
  "success": true,
  "message": "Ticket history fetched",
  "data": {
    "content": [
      {
        "id": "TKT-000012",
        "subject": "Payment issue",
        "message": "Payout delayed",
        "status": "OPEN",
        "categoryId": "PAYOUT",
        "bookingId": "BK-1",
        "priority": "HIGH",
        "createdAt": "2026-07-20T17:40:00",
        "updatedAt": "2026-07-20T17:40:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

### Partner Ticket Detail

```http
GET /api/v1/partner/support/tickets/TKT-000012
Authorization: Bearer <token>
```

Response:

```json
{
  "success": true,
  "message": "Ticket fetched",
  "data": {
    "id": "TKT-000012",
    "subject": "Payment issue",
    "status": "IN_PROGRESS",
    "categoryId": "PAYOUT",
    "bookingId": "BK-1",
    "priority": "HIGH",
    "createdAt": "2026-07-20T17:40:00",
    "updatedAt": "2026-07-20T17:45:00",
    "messages": [
      {
        "id": "MSG-1",
        "sender": "PARTNER",
        "message": "Payout delayed",
        "createdAt": "2026-07-20T17:40:00"
      },
      {
        "id": "MSG-2",
        "sender": "SUPPORT",
        "message": "We are checking this.",
        "createdAt": "2026-07-20T17:45:00"
      }
    ]
  }
}
```

## Admin App APIs

Base paths:

- `/api/v1/admin`
- `/api/admin`

Admin support uses separate list endpoints for customer and partner tickets. Detail, status update, and reply APIs are shared.

### Customer Support Tickets

```http
GET /api/v1/admin/support/customer/tickets?page=1&limit=10&search=refund&status=OPEN&categoryId=PAYMENT&priority=HIGH
Authorization: Bearer <admin-token>
```

### Partner Support Tickets

```http
GET /api/v1/admin/support/partner/tickets?page=1&limit=10&search=payout&status=OPEN&categoryId=PAYOUT&priority=HIGH
Authorization: Bearer <admin-token>
```

Query params:

- `page` default `1`
- `limit` default `10`, max `100`
- `search` optional, searches subject/message
- `status` optional: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `categoryId` optional
- `priority` optional: `LOW`, `MEDIUM`, `HIGH`, `URGENT`

List response:

```json
{
  "data": [
    {
      "id": 12,
      "ticketId": "TCK-10012",
      "requesterType": "PARTNER",
      "userId": 7,
      "subject": "Payment issue",
      "message": "Payout delayed",
      "status": "OPEN",
      "categoryId": "PAYOUT",
      "rawCategoryId": "PARTNER:PAYOUT",
      "bookingId": "BK-1",
      "priority": "HIGH",
      "createdAt": "2026-07-20T17:40:00",
      "updatedAt": "2026-07-20T17:40:00",
      "messages": [
        {
          "id": "MSG-1",
          "sender": "PARTNER",
          "message": "Payout delayed",
          "createdAt": "2026-07-20T17:40:00"
        }
      ]
    }
  ],
  "total": 1
}
```

Notes:

- For customer tickets, `requesterType` is `CUSTOMER`.
- For partner tickets, `requesterType` is `PARTNER`.
- Admin UI should display `categoryId`, not `rawCategoryId`.
- `rawCategoryId` is included for debugging/internal filtering.

### Ticket Detail

```http
GET /api/v1/admin/support/tickets/{id}
Authorization: Bearer <admin-token>
```

Use numeric `id`, not formatted `ticketId`.

Example:

```http
GET /api/v1/admin/support/tickets/12
```

### Update Ticket Status

```http
PATCH /api/v1/admin/support/tickets/{id}/status
Content-Type: application/json
Authorization: Bearer <admin-token>
```

Payload:

```json
{
  "status": "IN_PROGRESS"
}
```

Allowed statuses:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`

### Reply To Ticket

```http
POST /api/v1/admin/support/tickets/{id}/reply
Content-Type: application/json
Authorization: Bearer <admin-token>
```

Payload:

```json
{
  "message": "We are checking this."
}
```

Behavior:

- Adds a support reply.
- If ticket is not `CLOSED`, backend moves it to `IN_PROGRESS`.
- Reply appears in ticket messages with `sender: "SUPPORT"`.

## Recommended Frontend Tabs

Admin app:

- Customer Support tab: call `/api/v1/admin/support/customer/tickets`
- Partner Support tab: call `/api/v1/admin/support/partner/tickets`
- Detail drawer/page: call `/api/v1/admin/support/tickets/{id}`
- Reply modal: call `/api/v1/admin/support/tickets/{id}/reply`
- Status dropdown: call `/api/v1/admin/support/tickets/{id}/status`

Partner app:

- Support home/history: call `/api/v1/partner/support/tickets`
- Create ticket form: call `POST /api/v1/partner/support/tickets`
- Ticket detail/chat view: call `/api/v1/partner/support/tickets/{ticketId}`
