# Admin Frontend-Backend Integration

This repository contains admin backend APIs. Frontend project is not present here, so integration is done by wiring frontend calls to these endpoints.

## 1) Backend prerequisites

- Run backend on `http://localhost:8082` (or your deployed host).
- For physical phone testing, use your laptop LAN IP instead of localhost:
  - example: `http://192.168.1.23:8082`
  - phone and laptop must be on same Wi-Fi
- Ensure admin seed user exists:
  - `admin.seed.enabled=true`
  - default email/password from `application.properties`:
    - `admin@lexho.com` / `Admin@12345`
- Allow your frontend origin:
  - env var `APP_ALLOWED_CORS_ORIGINS`
  - example:
    - `APP_ALLOWED_CORS_ORIGINS=http://localhost:3000,http://localhost:5173,https://admin.yourdomain.com`

## 2) Auth flow (mandatory)

1. `POST /api/admin/login`
2. `POST /api/admin/verify-otp`
3. Read `accessToken` from verify response.
4. Send `Authorization: Bearer <accessToken>` for all protected `/api/admin/**` endpoints.

Important:
- Admin frontend must never call `/auth/login` for admin sign-in.
- Customer OTP/auth flows remain under `/otp/*` and `/auth/*`.

## 3) Ready client in this repo

- Use [integration/admin-api-client.js](C:/Users/shivendra/Downloads/lexho-user-service-master/integration/admin-api-client.js) in your frontend project.
- Initialize with your backend URL:

```js
import { createAdminApiClient } from "./admin-api-client";

const adminApi = createAdminApiClient({
  baseUrl: "http://192.168.1.23:8082",
});
```

If your frontend currently calls `/auth/login`, replace that call with:
- `adminApi.login(email, password)` -> `POST /api/admin/login`
- `adminApi.verifyOtp(email, otp)` -> `POST /api/admin/verify-otp`

Image upload:
- `adminApi.uploadImage(file)` -> `POST /api/admin/upload/image`
- multipart form-data field name: `file`
- response shape:
  - `{ "data": { "url": "/uploads/services/abc.jpg" } }`

## 4) Minimal frontend API client example (JavaScript)

```js
const BASE_URL = "http://192.168.1.23:8082";

async function adminLogin(email, password) {
  const res = await fetch(`${BASE_URL}/api/admin/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status}`);
  return res.json();
}

async function verifyAdminOtp(email, otp) {
  const res = await fetch(`${BASE_URL}/api/admin/verify-otp`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, otp }),
  });
  if (!res.ok) throw new Error(`OTP verify failed: ${res.status}`);
  const data = await res.json();
  localStorage.setItem("adminAccessToken", data.accessToken);
  return data;
}

async function adminApi(path, options = {}) {
  const token = localStorage.getItem("adminAccessToken");
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
    Authorization: `Bearer ${token}`,
  };
  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
  if (!res.ok) throw new Error(`API failed: ${res.status}`);
  return res.json();
}

// Examples:
// adminApi("/api/admin/dashboard/stats");
// adminApi("/api/admin/partners/1/verification", { method: "PATCH", body: JSON.stringify({ status: "VERIFIED" }) });
```

## 5) Smoke test command

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\admin-smoke.ps1
```

The script performs:
- login -> OTP
- verify OTP -> token
- dashboard stats API call
- partner verification `PATCH` API call
