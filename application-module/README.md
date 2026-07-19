# Emergency SOS Integration

## Overview

Emergency SOS flow is `admin-escalation only`.

When a partner triggers SOS:
- incident is saved
- live location is saved
- admin is notified
- emergency contacts are notified
- audit log is generated

Backend does not:
- call police directly
- send SMS to police directly

Any police escalation is handled manually by admin/ops team.

## Partner API

### Trigger SOS

`POST /api/v1/emergency/sos`

Headers:
```http
Authorization: Bearer <partner-jwt>
Content-Type: application/json
```

Request payload:
```json
{
  "latitude": 28.6139,
  "longitude": 77.2090,
  "accuracyMeters": 5.2,
  "address": "Connaught Place, New Delhi",
  "message": "Unsafe situation"
}
```

### SOS History

`GET /api/v1/emergency/history`

### Add Emergency Contact

`POST /api/v1/emergency/contact`

Request payload:
```json
{
  "name": "Amit Kumar",
  "phoneNumber": "9999999999",
  "relationship": "Brother",
  "email": "amit@example.com",
  "primaryContact": true
}
```

## Admin APIs

### List Incidents

`GET /api/v1/admin/emergency/incidents?page=1&limit=20&status=TRIGGERED&partnerId=7`

### Get Incident

`GET /api/v1/admin/emergency/incidents/{eventId}`

### Acknowledge Incident

`PATCH /api/v1/admin/emergency/incidents/{eventId}/acknowledge`

```json
{
  "note": "Ops team engaged"
}
```

### Resolve Incident

`PATCH /api/v1/admin/emergency/incidents/{eventId}/resolve`

```json
{
  "note": "Partner confirmed safe"
}
```

## Partner App Copy

Confirmation messages:
- `SOS alert sent to admin team.`
- `Your live location has been shared.`
- `Our admin team will review and take necessary action.`

Short toast:
- `SOS sent. Admin team has been alerted.`

## Admin UI Copy

Suggested labels:
- `Emergency SOS received`
- `Partner has triggered an SOS alert. Review the incident and take necessary action immediately.`
- `Location captured successfully`
- `Admin action required`
- `Acknowledge Incident`
- `Resolve Incident`

## API Notes

- `notifyPolice` request field has been removed
- partner app only sends location and optional message
- police-related backend dispatch flow has been removed from this module

## Config

```properties
app.emergency.max-contacts=5
app.notification.sms.enabled=true
app.notification.sms.gateway-url=https://sms-gateway.example/send
app.notification.sms.auth-token=secret
app.notification.sms.sender-id=LEXHO
app.notification.email.enabled=true
app.notification.email.from=no-reply@lexho.com
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=user
spring.mail.password=password
```

## Reference

Detailed API contract is also available in [EMERGENCY_SOS_API_CONTRACT.md](./EMERGENCY_SOS_API_CONTRACT.md).
