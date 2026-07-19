# Notification Frontend Contract

Base URL:

```text
http://localhost:8082
```

Use after login:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

## Device Token Registration

Use this in Customer, Partner, and Admin apps after login and whenever Firebase refreshes the FCM token.

```http
POST /api/notifications/devices
```

Customer payload:

```json
{
  "deviceId": "android-device-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "CUSTOMER"
}
```

Partner payload:

```json
{
  "deviceId": "android-device-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "PARTNER"
}
```

Admin payload:

```json
{
  "deviceId": "android-device-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "ADMIN"
}
```

Remove token on logout:

```http
DELETE /api/notifications/devices/{deviceId}
```

## Admin Send Notification

```http
POST /api/admin/notifications/send
```

Booking confirmed:

```json
{
  "title": "Booking Confirmed",
  "message": "Your booking has been confirmed.",
  "notificationType": "BOOKING_CONFIRMED",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "BOOKING",
  "referenceId": 123,
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Professional assigned:

```json
{
  "title": "Professional Assigned",
  "message": "A professional has been assigned to your booking.",
  "notificationType": "PROFESSIONAL_ASSIGNED",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Professional on the way:

```json
{
  "title": "Professional On The Way",
  "message": "Your professional is on the way.",
  "notificationType": "PROFESSIONAL_ON_THE_WAY",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Service started:

```json
{
  "title": "Service Started",
  "message": "Your service has started.",
  "notificationType": "SERVICE_STARTED",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Service completed:

```json
{
  "title": "Service Completed",
  "message": "Your service is completed. Please rate your experience.",
  "notificationType": "SERVICE_COMPLETED",
  "priority": "MEDIUM",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Payment successful:

```json
{
  "title": "Payment Successful",
  "message": "Your payment was successful.",
  "notificationType": "PAYMENT_SUCCESSFUL",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "PAYMENT",
  "redirectType": "PAYMENT",
  "redirectValue": "987"
}
```

Subscription reminder:

```json
{
  "title": "Subscription Reminder",
  "message": "Your subscription visit is scheduled tomorrow.",
  "notificationType": "SUBSCRIPTION_REMINDER",
  "priority": "MEDIUM",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101],
  "referenceType": "SUBSCRIPTION",
  "redirectType": "SUBSCRIPTION",
  "redirectValue": "555"
}
```

Promotional offer:

```json
{
  "title": "Special Offer",
  "message": "Get 20% off on your next booking.",
  "notificationType": "PROMOTIONAL_OFFER",
  "priority": "MEDIUM",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "referenceType": "PROMOTION",
  "redirectType": "PROMOTION",
  "redirectValue": "OFFER20"
}
```

Partner notification:

```json
{
  "title": "New Booking",
  "message": "A new booking is available.",
  "notificationType": "BOOKING",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "PARTNER",
  "userIds": [501],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Admin app notification:

```json
{
  "title": "Payment Review Required",
  "message": "A payment needs admin review.",
  "notificationType": "PAYMENT",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "ADMIN",
  "userIds": [1],
  "referenceType": "PAYMENT",
  "redirectType": "PAYMENT",
  "redirectValue": "987"
}
```

## Notification List APIs

Customer:

```http
GET /api/customer/notifications?page=0&size=20
```

Partner:

```http
GET /api/provider/notifications?page=0&size=20
```

Admin:

```http
GET /api/admin/notifications?page=0&size=20
```

Admin filters:

```http
GET /api/admin/notifications?page=0&size=20&userType=CUSTOMER&notificationType=BOOKING_CONFIRMED&channelType=PUSH&status=SENT&search=booking
```

Admin detail:

```http
GET /api/admin/notifications/{id}
```

Retry failed notification:

```http
POST /api/admin/notifications/{id}/retry
```

Stats:

```http
GET /api/admin/notifications/stats
```

## Read And Delete

Customer:

```http
PATCH /api/customer/notifications/{id}/read
DELETE /api/customer/notifications/{id}
```

Partner:

```http
PATCH /api/provider/notifications/{id}/read
DELETE /api/provider/notifications/{id}
```

## FCM Tap Payload

Android receives data like:

```json
{
  "notificationId": "9001",
  "type": "BOOKING_CONFIRMED",
  "notificationType": "BOOKING_CONFIRMED",
  "userType": "CUSTOMER",
  "referenceType": "BOOKING",
  "referenceId": "123",
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Recommended routing:

```text
BOOKING -> booking detail/tracking
PAYMENT -> payment status
SUBSCRIPTION -> subscription detail
PROMOTION -> offers
NOTIFICATIONS -> notification center
```

## Allowed Values

Notification types:

```text
BOOKING
BOOKING_CONFIRMED
PROFESSIONAL_ASSIGNED
PROFESSIONAL_ON_THE_WAY
SERVICE_STARTED
SERVICE_COMPLETED
PAYMENT
PAYMENT_SUCCESSFUL
PROMOTION
PROMOTIONAL_OFFER
SYSTEM
REVIEW
REMINDER
SUBSCRIPTION_REMINDER
```

Channels:

```text
PUSH
IN_APP
EMAIL
SMS
WHATSAPP
```

Target user types:

```text
CUSTOMER
PARTNER
ADMIN
```

Priority:

```text
LOW
MEDIUM
HIGH
CRITICAL
```
