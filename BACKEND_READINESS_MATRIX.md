# Backend Readiness Matrix (Customer / Partner / Admin)

Date: 2026-05-22

Base URL: `http://localhost:8082`

## Customer App

| Screen | Status | Backend |
|---|---|---|
| Login (OTP) | Done | `/otp/send`, `/otp/verify` |
| Home dashboard | Done | `/api/home/dashboard`, banners/categories/services endpoints |
| Service listing + search | Done | `/api/services/popular`, `/api/services/recommended`, `/api/services/quick`, `/api/search/services` |
| Service detail | Partial | list/search data available, dedicated rich detail endpoint limited |
| Cart | Done | `/api/cart/add`, `/api/cart/summary`, `/api/cart/update`, `/api/cart/remove` |
| Address management | Done | `/api/v1/customer/location/*` + legacy `/api/v1/address/*` |
| Slot selection + booking | Done | `/api/v1/bookings/create`, slot validation logic present |
| Booking confirmation | Done | booking create response payload ready |
| Live booking tracking | Partial | booking status + dispatch events exist; full realtime customer map tracking endpoint set limited |
| Booking history/detail | Done | booking list/detail APIs in experience/order flow |
| Payments | Partial | payment option/initiate/refund APIs present; full gateway production flow depends on integration |
| Notifications | Done | customer notification fetch flow available |
| Profile | Partial | profile APIs exist in modules, but full settings depth may vary |
| Ratings & reviews | Done | review add/list/update flows present |
| Support tickets + FAQ | Done | support create/list/detail + FAQ endpoints present |

## Partner App

| Screen | Status | Backend |
|---|---|---|
| Login (OTP) | Done | `/otp/send`, `/otp/verify` (`role=PARTNER`) |
| Availability toggle | Done | `/api/v1/partner/availability`, `/api/v1/partner/me/status` |
| Shift start/end | Done | `/api/v1/partner/dispatch/shift/start`, `/shift/end` |
| Live location update | Done | `/api/v1/partner/dispatch/location` |
| Incoming requests queue | Done | `/api/v1/partner/requests*`, accept/reject APIs |
| New orders list | Done | `/api/v1/orders/new` |
| Active job actions | Done | `/api/v1/orders/{id}/start`, `/complete`, accept/reject |
| Order history | Done | `/api/v1/orders/history` |
| Dashboard stats | Done | `/api/v1/partner/dashboard/stats`, `/api/v1/orders/dashboard` |
| Skills management | Done | `/api/v1/partner/dispatch/skills` |
| Notifications | Done | `/api/v1/partner/notifications` |
| Earnings summary | Partial/Missing | basic order data exists, dedicated payout/settlement module not complete |

## Admin App

| Screen | Status | Backend |
|---|---|---|
| Login (email + OTP) | Done | `/api/admin/login`, `/api/admin/verify-otp` |
| Notification center | Done | `/api/admin/notifications/*`, `/send`, `/stats`, retry |
| City management | Done | `/api/v1/admin/geo/cities*` |
| Zone management (polygon) | Done | `/api/v1/admin/geo/zones*` |
| Serviceable area management | Done | `/api/v1/admin/serviceable-areas*` |
| Dispatch monitor/manual dispatch | Done | `/api/admin/dispatch/orders/{orderId}`, lifecycle patch |
| Partner control (availability/stats) | Done | `/api/v1/admin/partners/*` |
| Booking management | Partial | core booking/dispatch/order APIs available; full admin booking console depth partial |
| Customer management | Partial | customer data APIs exist in parts; full admin CRUD/filters may be partial |
| Category/service/content mgmt | Partial to Done | many endpoints exist, but final coverage depends on exact UI scope |
| Reviews moderation | Partial | review APIs exist; full moderation workflow may need more |
| Support/FAQ management | Done | support ticket + FAQ admin flows present |
| Global settings/radius configs UI APIs | Partial | config is property-driven; complete admin runtime settings CRUD limited |

## Map / Geo Readiness

| Capability | Status | Backend |
|---|---|---|
| Mapbox geocoding | Done | `/api/v1/maps/geocode` |
| OSM reverse geocode/serviceability | Done | customer location module |
| Geofence validation in booking | Done | booking path integrated |
| Full route ETA/navigation APIs | Partial/Missing | not fully exposed as dedicated module yet |
