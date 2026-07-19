# Firebase Backend Integration

This backend already provides the server-side contract for Firebase Cloud Messaging. Android apps still need to integrate Firebase SDKs, obtain FCM tokens, and register those tokens with this backend.

## Backend Configuration

Enable FCM in the backend environment:

```properties
APP_PUSH_FCM_ENABLED=true
APP_PUSH_FCM_SERVICE_ACCOUNT_PATH=/secure/path/firebase-service-account.json
```

Alternatively:

```properties
APP_PUSH_FCM_ENABLED=true
APP_PUSH_FCM_SERVICE_ACCOUNT_BASE64=<base64 encoded Firebase service account JSON>
```

If FCM is disabled or credentials are missing, `PUSH` notifications are saved but dispatch fails with a logged failure reason.

## Device Token Registration

Call after login and whenever Firebase issues a refreshed token.

```http
POST /api/notifications/devices
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Customer app payload:

```json
{
  "deviceId": "android-secure-device-id-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "CUSTOMER"
}
```

Partner app payload:

```json
{
  "deviceId": "android-secure-device-id-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "PARTNER"
}
```

Admin app payload:

```json
{
  "deviceId": "android-secure-device-id-or-installation-id",
  "fcmToken": "firebase-fcm-token",
  "platform": "android",
  "appType": "ADMIN"
}
```

Unregister on logout if the app wants to stop receiving user-specific push on that device:

```http
DELETE /api/notifications/devices/{deviceId}
Authorization: Bearer <accessToken>
```

## Supported Notification Types

Backend enum values:

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

Legacy aliases accepted by admin API:

```text
OFFER -> PROMOTIONAL_OFFER
OFFERS -> PROMOTIONAL_OFFER
PROMOTIONAL_OFFERS -> PROMOTIONAL_OFFER
PAYMENT_SUCCESS -> PAYMENT_SUCCESSFUL
```

## Admin Send API

```http
POST /api/admin/notifications/send
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

Send to all customers:

```json
{
  "title": "Booking Confirmed",
  "message": "Your booking has been confirmed.",
  "notificationType": "BOOKING_CONFIRMED",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "referenceType": "BOOKING",
  "referenceId": 123,
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Send to specific users:

```json
{
  "title": "Professional Assigned",
  "message": "A professional has been assigned to your booking.",
  "notificationType": "PROFESSIONAL_ASSIGNED",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "CUSTOMER",
  "userIds": [101, 102],
  "referenceType": "BOOKING",
  "redirectType": "BOOKING",
  "redirectValue": "BK-2026-0001"
}
```

Partner notification:

```json
{
  "title": "New Booking",
  "message": "A booking needs your attention.",
  "notificationType": "BOOKING",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "PARTNER",
  "userIds": [501],
  "redirectType": "BOOKING",
  "redirectValue": "123"
}
```

Admin app notification:

```json
{
  "title": "Payment Failed",
  "message": "A payment requires review.",
  "notificationType": "PAYMENT",
  "priority": "HIGH",
  "channels": ["PUSH", "IN_APP"],
  "targetUserType": "ADMIN",
  "userIds": [1],
  "redirectType": "PAYMENT",
  "redirectValue": "987"
}
```

## FCM Data Payload

The backend sends a notification payload plus this data payload:

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

Android should route tap events using `redirectType` and `redirectValue`.

Recommended routing:

```text
BOOKING -> booking detail/tracking screen
PAYMENT -> payment status screen
SUBSCRIPTION -> subscription detail screen
PROMOTION -> offers screen
NOTIFICATIONS -> notification center
```

## Testing Guide

1. Configure Firebase service account in backend env.
2. Start backend.
3. Login in Customer, Partner, or Admin app.
4. Register FCM token using `POST /api/notifications/devices`.
5. Send admin notification with `channels: ["PUSH", "IN_APP"]`.
6. Verify app receives notification in foreground.
7. Put app in background and send again.
8. Kill app and send again.
9. Tap notification and verify screen routing from `redirectType` and `redirectValue`.
10. Check admin notification list for `SENT` or `FAILED` plus failure reason.

## Android Responsibilities

Android app must implement:

- Firebase Messaging SDK setup.
- `FirebaseMessagingService.onNewToken`.
- Token registration after login and token refresh.
- Runtime notification permission on Android 13+.
- Notification channel creation on Android 8+.
- Foreground notification rendering.
- Background/terminated tap routing.
- Firebase Analytics events.
- Crashlytics automatic and non-fatal crash reporting.
