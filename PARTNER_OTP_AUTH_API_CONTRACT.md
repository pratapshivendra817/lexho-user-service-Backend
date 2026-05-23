# Partner OTP Auth API Contract

Base URL examples:
- `http://<host>:8082`

All responses follow:

```json
{
  "success": true,
  "message": "text",
  "data": {},
  "token": "optional-jwt",
  "timestamp": "2026-05-11T13:14:50.559148400+05:30"
}
```

## 1) Send OTP (Partner)

`POST /otp/send`

Request:

```json
{
  "mobile": "9876543210",
  "role": "PARTNER"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "mobile": "9876543210",
    "resendAfter": 30,
    "role": "PARTNER",
    "otp": "123456"
  },
  "token": null,
  "timestamp": "..."
}
```

## 2) Verify OTP (Partner)

`POST /otp/verify`

Request:

```json
{
  "mobile": "9876543210",
  "otp": "123456",
  "role": "PARTNER"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "token": "<access_jwt>",
    "refreshToken": "<refresh_token>",
    "user": {
      "id": 1,
      "name": "Lexho User",
      "mobile": "9876543210",
      "email": null,
      "avatar": null,
      "role": "PARTNER"
    }
  },
  "token": "<access_jwt>",
  "timestamp": "..."
}
```

## 3) Refresh Access Token

Preferred endpoint:

`POST /api/auth/refresh`

Backward-compatible endpoint:

`POST /auth/refresh-token`

Request schema (`deviceId` optional):

```json
{
  "refreshToken": "<refresh_token>",
  "deviceId": "<device_id>"
}
```

Success `200` (`/api/auth/refresh`):

```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "<new_access_jwt>"
  },
  "token": "<new_access_jwt>",
  "timestamp": "..."
}
```

Success `200` (`/auth/refresh-token`):

```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "token": "<new_access_jwt>"
  },
  "token": "<new_access_jwt>",
  "timestamp": "..."
}
```

## Auth Header Standard

For protected APIs:

`Authorization: Bearer <access_token>`

## Error Codes

### `400 Bad Request`
- Invalid payload
- Missing required fields
- Invalid role / invalid OTP format
- Invalid refresh token payload

Example:

```json
{
  "success": false,
  "message": "Invalid request",
  "data": null,
  "token": null,
  "timestamp": "..."
}
```

### `401 Unauthorized`
- Missing bearer token
- Invalid/expired token

Example:

```json
{
  "success": false,
  "message": "Authorization token is required",
  "data": null,
  "token": null,
  "timestamp": "..."
}
```

### `429 Too Many Requests`
- OTP send/verify rate limit exceeded

Example:

```json
{
  "success": false,
  "message": "Too many OTP requests. Please try later",
  "data": null,
  "token": null,
  "timestamp": "..."
}
```

