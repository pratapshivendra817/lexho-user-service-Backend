# Partner Payment Integration

Use the existing partner JWT in the `Authorization: Bearer <token>` header.

## Partner APIs

- `GET /api/v1/partner/earnings/summary`
- `GET /api/v1/partner/earnings/transactions?page=0&size=20`
- `GET /api/v1/partner/payouts?page=0&size=20`
- `GET /api/v1/partner/payouts/{payoutId}`
- `GET /api/v1/partner/bank-details`
- `PUT /api/v1/partner/bank-details`
- `POST /api/v1/partner/withdrawals`
- `GET /api/v1/partner/withdrawals?page=0&size=20`
- `GET /api/v1/partner/withdrawals/balance`

Bank details update body:

```json
{
  "accountNumber": "123456789012",
  "ifscCode": "HDFC0001234"
}
```

Withdrawal request body:

```json
{
  "amount": 500.00
}
```

## UI Screens

- Earnings dashboard with today, week, month and lifetime settlement values.
- Payout history and payout detail with gross amount, commission, reversal and status.
- Bank account screen. Display only the masked account number returned by the API.
- Withdrawal screen with available balance validation and withdrawal history.

## Settlement Rules

- A payout ledger row is generated only after both payment success and service completion.
- Default commission is controlled by `PARTNER_COMMISSION_PERCENT`.
- Refunds reverse the partner settlement proportionally.
- Withdrawals use settled balance after refund reversals and existing withdrawal reservations.
