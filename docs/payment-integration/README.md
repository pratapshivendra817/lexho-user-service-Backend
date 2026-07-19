# Lexho Razorpay Integration

## Existing Components Reused

- The monolith already persists customer bookings in `orders`. A second booking table is not introduced.
- Existing customer booking APIs remain unchanged.
- Existing legacy `/api/payments/options`, `/api/payments/initiate`, `/api/payments/status/{bookingId}` and `/api/payments/refund` endpoints remain available for compatibility.

## Backend Structure

```text
payment-service/src/main/java/com/lexho/payment/
  config/       Razorpay environment configuration
  controller/   Customer payment APIs
  dto/          Validated requests and stable responses
  entity/       Payment attempts and webhook idempotency
  exception/    Payment API error handling
  repository/   JPA repositories
  service/      Razorpay client, HMAC validation and transactional workflow
  webhook/      Public signature-protected webhook endpoint
```

## Environment

Set these values in the root `.env` file. Never commit real secrets.

```properties
RAZORPAY_KEY_ID=rzp_test_replace_me
RAZORPAY_KEY_SECRET=replace_me
RAZORPAY_WEBHOOK_SECRET=replace_me
RAZORPAY_CURRENCY=INR
```

Configure the Razorpay dashboard webhook URL:

```text
https://your-api-host/api/payments/webhook
```

Enable `payment.captured`, `payment.failed` and `refund.processed`. Use dashboard auto-capture so a customer booking is confirmed only after a captured payment.

## Customer Flow

1. Create a booking using the existing booking API. The existing order starts with payment status `PENDING`.
2. Call `POST /api/payments/create-order` with a stable `Idempotency-Key` header for the current attempt.
3. Open Razorpay Checkout using `keyId`, `razorpayOrderId`, `amountInPaise` and `currency`.
4. Send the Checkout success fields to `POST /api/payments/verify`.
5. The backend verifies the HMAC signature and fetches the payment from Razorpay to validate its order ID, amount and capture state.
6. A captured payment marks the attempt `PAID`, updates the existing order payment status to `SUCCESS`, and moves a newly-created order to `CONFIRMED`.
7. Razorpay webhooks reconcile asynchronous capture, failure and processed refund events.

Supported checkout methods are controlled in Razorpay Standard Checkout: UPI, cards, net banking and wallets.

## APIs

### Create Razorpay Order

```http
POST /api/payments/create-order
Authorization: Bearer <customer-jwt>
Idempotency-Key: booking-412-attempt-1
Content-Type: application/json

{"bookingId":412}
```

```json
{
  "success": true,
  "message": "Razorpay order created",
  "data": {
    "bookingId": 412,
    "razorpayOrderId": "order_Q123",
    "keyId": "rzp_test_example",
    "amountInPaise": 525000,
    "amount": 5250.00,
    "currency": "INR",
    "status": "CREATED"
  }
}
```

### Verify Checkout Success

```http
POST /api/payments/verify
Authorization: Bearer <customer-jwt>
Content-Type: application/json

{
  "razorpayOrderId": "order_Q123",
  "razorpayPaymentId": "pay_Q123",
  "razorpaySignature": "<checkout-signature>"
}
```

```json
{
  "success": true,
  "message": "Payment verified",
  "data": {
    "bookingId": 412,
    "razorpayOrderId": "order_Q123",
    "razorpayPaymentId": "pay_Q123",
    "amount": 5250.00,
    "currency": "INR",
    "paymentStatus": "PAID",
    "verified": true
  }
}
```

### Fetch Latest Attempt

```http
GET /api/payments/412
Authorization: Bearer <customer-jwt>
```

### Webhook

```http
POST /api/payments/webhook
X-Razorpay-Event-Id: <unique-event-id>
X-Razorpay-Signature: <webhook-signature>
```

The webhook validates the signature against the unmodified raw request body. Repeated event IDs are accepted without processing the event twice.

## Retry Strategy

- Reuse the same `Idempotency-Key` while retrying the same HTTP create-order request after timeout or network failure.
- Generate a new idempotency key when the user deliberately starts a fresh checkout attempt.
- Create a new Razorpay order for a fresh attempt. Preserve previous attempts for audit and reconciliation.
- Check `GET /api/payments/{bookingId}` before showing an unpaid state after an ambiguous client timeout.

## Refund Design

The current webhook handles `refund.processed` reconciliation. The next admin/customer refund command should:

1. Validate cancellation policy and refundable amount.
2. Create a refund through Razorpay Refunds API using the stored payment ID.
3. Persist a separate `payment_refunds` row with requested, processed and failed states.
4. Update the row from `refund.processed` and refund-failure webhooks.
5. Keep booking cancellation separate from refund settlement because refunds are asynchronous.

## Production Notes

- Do not trust an amount submitted by the browser. The backend reads the amount from the booking.
- Do not mark a booking paid only because Checkout called the frontend handler. Verify the signature and reconcile with webhooks.
- Do not log secrets, signatures or full webhook payloads.
- Keep webhook signature verification on the raw request body.
- Keep a unique event ID table because Razorpay can deliver duplicate and out-of-order webhooks.
- Rotate webhook secrets carefully. Razorpay retries older webhooks signed with the previous secret.
- Add metrics and alerts for failed order creation, invalid signatures, repeated webhook failures and payments stuck in `CREATED`.

## Future Scaling

- Add an outbox table for reliable notification dispatch after payment state changes.
- Add a scheduled reconciliation job for payment attempts stuck in `CREATED`.
- Add a dedicated refund table before exposing refunds to customers.
- Move payment processing behind an internal boundary only when the monolith load justifies extraction.
