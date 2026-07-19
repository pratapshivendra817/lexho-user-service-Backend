# Navigation Live ETA Contract

## WebSocket endpoint

Connect to:

- `/ws-notifications`

This endpoint is registered in
[`NotificationWebSocketConfig`](../src/main/java/com/lexho/user_service/notification/config/NotificationWebSocketConfig.java).

## STOMP subscriptions

Subscribe to either topic:

- `/topic/navigation/eta/{bookingId}`
- `/topic/admin/bookings/{bookingId}/eta`
- `/topic/customer/bookings/{bookingId}/eta`

Example:

- `/topic/navigation/eta/77`
- `/topic/admin/bookings/77/eta`

## Activation flow

1. Call `GET /api/v1/navigation/eta/{bookingId}`
2. Backend registers the booking for live ETA tracking
3. Scheduler refreshes ETA periodically
4. Updated ETA is pushed to the subscribed topic

## Admin activation flow

Admin app can use either of these endpoints:

1. `GET /api/v1/admin/booking-management/{bookingId}/navigation`
2. `GET /api/v1/admin/booking-management/{bookingId}/tracking/live`
3. `PATCH /api/v1/admin/booking-management/{bookingId}/tracking/partner-location`

Admin override updates the stored partner location, recomputes navigation, and pushes the refreshed ETA to:

- `/topic/admin/bookings/{bookingId}/eta`

## Message payload

```json
{
  "bookingId": 77,
  "distanceKm": 4.2,
  "durationMinutes": 12,
  "polyline": "...",
  "eta": "2026-06-14T20:45:00",
  "trafficAware": true,
  "provider": "MAPBOX",
  "cacheHit": false,
  "routeRecalculated": true,
  "routeDeviationDetected": false,
  "deviationMeters": null,
  "alternativesConsidered": 2,
  "liveTrackingEnabled": true,
  "partnerLatitude": 28.61,
  "partnerLongitude": 77.20,
  "partnerLocationUpdatedAt": "2026-06-14T20:33:00",
  "customerLatitude": 28.62,
  "customerLongitude": 77.21,
  "routeComputedAt": "2026-06-14T20:34:00"
}
```

## Client behavior

1. Fetch the ETA once when the tracking screen opens.
2. Open a STOMP connection to `/ws-notifications`.
3. Subscribe to the booking topic used by your surface:
   - customer/partner: `/topic/navigation/eta/{bookingId}`
   - admin: `/topic/admin/bookings/{bookingId}/eta`
4. Replace displayed ETA, distance, duration, and route polyline whenever a new message arrives.
5. Unsubscribe when the tracking screen closes.
