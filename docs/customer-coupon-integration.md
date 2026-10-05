# Customer Coupon Integration

Customer app coupon validation should call the backend before showing a discounted payable amount.

## Reward Coupons

```http
GET /api/coupons/my-rewards?page=0&size=20
Authorization: Bearer <customerToken>
```

Use `data.content` to list referral reward coupons assigned to the logged-in customer.

## Validate Coupon

```http
POST /api/coupons/validate
Authorization: Bearer <customerToken>
Content-Type: application/json

{
  "code": "FIRSTONLY",
  "planId": 1,
  "amount": 499.00,
  "bookingType": "INSTANT"
}
```

The backend enforces first-booking eligibility. For admin-created coupons, customers with any previous successful paid booking receive:

```json
{
  "success": false,
  "message": "Coupon is valid only for first booking"
}
```

On success, use `data.discountAmount` and `data.finalAmount` for the booking/payment UI. Do not calculate first-booking eligibility in the client.
