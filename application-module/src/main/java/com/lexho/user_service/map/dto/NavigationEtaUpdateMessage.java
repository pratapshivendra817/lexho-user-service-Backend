package com.lexho.user_service.map.dto;

import java.time.LocalDateTime;

public record NavigationEtaUpdateMessage(
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

    public static NavigationEtaUpdateMessage from(NavigationRouteResponse response) {
        return new NavigationEtaUpdateMessage(
                response.bookingId(),
                response.distanceKm(),
                response.durationMinutes(),
                response.polyline(),
                response.eta(),
                response.trafficAware(),
                response.provider(),
                response.cacheHit(),
                response.routeRecalculated(),
                response.routeDeviationDetected(),
                response.deviationMeters(),
                response.alternativesConsidered(),
                response.liveTrackingEnabled(),
                response.partnerLatitude(),
                response.partnerLongitude(),
                response.partnerLocationUpdatedAt(),
                response.customerLatitude(),
                response.customerLongitude(),
                response.routeComputedAt()
        );
    }
}
