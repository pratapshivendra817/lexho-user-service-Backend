# Admin Payment Integration

All endpoints require an admin JWT:

```http
Authorization: Bearer <admin-token>
```

## Dashboard

```http
GET /api/v1/admin/payments/dashboard
```

Returns attempt counts, paid amount and processed refund amount.

## Transaction Attempts

```http
GET /api/v1/admin/payments/transactions?bookingId=78&status=PAID&page=0&size=20
```

Filters are optional. Supported statuses: `CREATED`, `PAID`, `FAILED`, `REFUNDED`.

## Booking Payment Detail

```http
GET /api/v1/admin/payments/bookings/78
```

Returns booking payment status, Razorpay attempt history and refund history.

## Reconcile Latest Razorpay Payment

Use this for a payment stuck in `CREATED` after a customer-side network interruption:

```http
POST /api/v1/admin/payments/bookings/78/reconcile
```

The backend fetches the latest payment from Razorpay and confirms the booking only
after validating the Razorpay order ID, payment ID and amount.

## Create Refund

```http
POST /api/v1/admin/payments/bookings/78/refunds
Idempotency-Key: refund-booking-78-<uuid>
Content-Type: application/json

{
  "amount": 299.00,
  "reason": "Customer cancellation approved"
}
```

The backend calls Razorpay Refund API and stores the refund. Partial refunds are
supported. A booking becomes `REFUNDED` only when processed refunds cover the
captured payment amount.

## Existing Booking Admin APIs

```http
GET   /api/v1/admin/booking-management/{bookingId}/payment/reconciliation
PUT   /api/v1/admin/booking-management/{bookingId}/payment/payout
PATCH /api/v1/admin/booking-management/{bookingId}/payment/status
```

The manual status override only permits `PENDING` and `FAILED`. It requires a
reason and writes an audit event:

```json
{
  "paymentStatus": "FAILED",
  "reason": "Manual review confirmed failure"
}
```

`SUCCESS` and `REFUNDED` are webhook or Razorpay reconciliation states.
