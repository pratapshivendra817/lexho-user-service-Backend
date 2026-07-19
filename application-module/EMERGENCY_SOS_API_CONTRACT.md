# Emergency SOS API Contract

## Partner APIs

### Trigger SOS

`POST /api/v1/emergency/sos`

```json
{
  "latitude": 28.6139,
  "longitude": 77.2090,
  "accuracyMeters": 5.2,
  "address": "Connaught Place, New Delhi",
  "message": "Unsafe situation"
}
```

Behavior:
- creates `emergency_event`
- creates `emergency_location`
- sends admin alert
- notifies active emergency contacts through configured SMS/email channels
- does not contact police directly from backend
- admin team reviews the incident and takes necessary action manually
- writes audit log

### Emergency history

`GET /api/v1/emergency/history`

Returns partner-owned incidents with latest and captured locations.

### Add emergency contact

`POST /api/v1/emergency/contact`

```json
{
  "name": "Amit Kumar",
  "phoneNumber": "9999999999",
  "relationship": "Brother",
  "email": "amit@example.com",
  "primaryContact": true
}
```

Rules:
- max contacts configurable by `app.emergency.max-contacts`
- duplicate active phone numbers are rejected

## Admin APIs

### List incidents

`GET /api/v1/admin/emergency/incidents?page=1&limit=20&status=TRIGGERED&partnerId=7`

### Get incident

`GET /api/v1/admin/emergency/incidents/{eventId}`

### Acknowledge incident

`PATCH /api/v1/admin/emergency/incidents/{eventId}/acknowledge`

```json
{
  "note": "Ops team engaged"
}
```

### Resolve incident

`PATCH /api/v1/admin/emergency/incidents/{eventId}/resolve`

```json
{
  "note": "Partner confirmed safe"
}
```

## Required config

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

## Operational model

- SOS backend is `admin-escalation only`
- no direct police call is triggered
- no direct police SMS is triggered
- any police escalation happens manually by admin/ops team outside the app

## API note

- `notifyPolice` request field has been removed from the API contract
- partner app only sends incident location and optional message
