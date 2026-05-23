# Frontend Token Matrix

Base URL: `http://localhost:8082`

## Public (no token required when exposure flag allows)
- `POST /api/admin/login`
- `POST /api/admin/verify-otp`
- `GET /api/customer/profile-content`
- `GET /api/customer/home/sections`
- `GET /api/customer/services`
- `GET /api/customer/services/**`
- `GET /api/customer/banners`
- `GET /api/banners/**`
- `GET /api/categories/**`

## Admin token required (`ROLE_ADMIN`)
- `/api/admin/**` (except login + verify-otp, and profile-content if configured public)
- `GET /api/ops/summary` (admin or partner)

## Customer token required (`ROLE_CUSTOMER`)
- `/api/customer/**` write endpoints (cart/profile/addresses etc.)
- All `/api/customer/**` when `APP_SECURITY_ALLOW_PUBLIC_CUSTOMER_READ=false`

## Partner / Partner token required
- `/api/partner/**` => `ROLE_PARTNER`
- `/api/partner/**` => `ROLE_PARTNER` or `ROLE_PROVIDER`

## Internal API behavior
- `/internal/**`:
  - If `APP_SECURITY_ALLOW_INTERNAL_OPEN=true`: open
  - If `APP_SECURITY_ALLOW_INTERNAL_OPEN=false`: requires header `X-Internal-Token: <INTERNAL_API_TOKEN>`

## Frontend implementation checklist
- Keep separate token storage keys for admin/customer/partner.
- Attach `Authorization: Bearer <token>` via interceptor for protected routes.
- On 401: clear corresponding token and redirect to login flow.
- On 403: show role/access error (not generic network failure).

## Axios interceptor example
```ts
import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082",
});

api.interceptors.request.use((config) => {
  const role = localStorage.getItem("active_role"); // admin|customer|partner
  const tokenKey =
    role === "admin"
      ? "admin_access_token"
      : role === "partner"
      ? "partner_access_token"
      : "customer_access_token";
  const token = localStorage.getItem(tokenKey);
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      // token invalid/expired
      localStorage.removeItem("admin_access_token");
      localStorage.removeItem("customer_access_token");
      localStorage.removeItem("partner_access_token");
    }
    return Promise.reject(error);
  }
);

export default api;
```
