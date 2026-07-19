package com.lexho.user_service.map.dto;

import java.time.LocalDateTime;

public record NavigationRouteResponse(
        Long bookingId,
        Double distanceKm,
        Integer durationMinutes,
        String polyline,
        LocalDateTime eta,
        Boolean trafficAware,
        String provider,
        Boolean cacheHit,
        Boolean routeRecalculated,
        Boolean routeDeviationDetected,
        Double deviationMeters,
        Integer alternativesConsidered,
        Boolean liveTrackingEnabled,
        Double partnerLatitude,
        Double partnerLongitude,
        LocalDateTime partnerLocationUpdatedAt,
        Double customerLatitude,
        Double customerLongitude,
        LocalDateTime routeComputedAt
) {
}
