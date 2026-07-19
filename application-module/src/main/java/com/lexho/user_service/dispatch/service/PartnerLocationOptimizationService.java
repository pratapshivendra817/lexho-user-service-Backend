package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.dispatch.dto.PartnerLocationUpdateRequest;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PartnerLocationOptimizationService {
    private final double stationaryThresholdMeters;
    private final double poorAccuracyMeters;
    private final double lowSpeedKmh;
    private final double headingDeltaThreshold;
    private final double impossibleSpeedKmh;

    public PartnerLocationOptimizationService(
            @Value("${app.tracking.stationary-threshold-meters:20}") double stationaryThresholdMeters,
            @Value("${app.tracking.poor-accuracy-meters:80}") double poorAccuracyMeters,
            @Value("${app.tracking.low-speed-kmh:2.5}") double lowSpeedKmh,
            @Value("${app.tracking.heading-delta-threshold-deg:7}") double headingDeltaThreshold,
            @Value("${app.location.risk.impossible-jump-speed-kmh:300}") double impossibleSpeedKmh) {
        this.stationaryThresholdMeters = Math.max(5.0d, Math.min(100.0d, stationaryThresholdMeters));
        this.poorAccuracyMeters = Math.max(20.0d, Math.min(300.0d, poorAccuracyMeters));
        this.lowSpeedKmh = Math.max(0.5d, Math.min(20.0d, lowSpeedKmh));
        this.headingDeltaThreshold = Math.max(2.0d, Math.min(45.0d, headingDeltaThreshold));
        this.impossibleSpeedKmh = Math.max(80.0d, Math.min(900.0d, impossibleSpeedKmh));
    }

    public Decision evaluate(CurrentPartnerLocationEntity previous, PartnerLocationUpdateRequest request) {
        double accuracy = request.accuracyMeters == null ? 0.0d : Math.max(0.0d, request.accuracyMeters);
        if (accuracy > poorAccuracyMeters) {
            return new Decision(false, true, true, "POOR_ACCURACY");
        }
        if (previous == null || previous.getLatitude() == null || previous.getLongitude() == null || previous.getUpdatedAt() == null) {
            return new Decision(true, true, false, null);
        }

        double movedMeters = haversineMeters(
                previous.getLatitude().doubleValue(),
                previous.getLongitude().doubleValue(),
                request.latitude,
                request.longitude
        );
        boolean lowSpeed = request.speedKmh != null && request.speedKmh <= lowSpeedKmh;
        boolean headingStable = isHeadingStable(previous.getHeadingDegrees(), request.headingDegrees);
        boolean belowMoveThreshold = movedMeters < stationaryThresholdMeters;
        boolean heartbeatOnly = belowMoveThreshold && lowSpeed && headingStable;
        boolean significantMove = !heartbeatOnly;

        boolean suspicious = isSuspiciousJump(previous, request, movedMeters);
        String reason = null;
        if (suspicious) {
            reason = "IMPOSSIBLE_SPEED";
        } else if (heartbeatOnly) {
            reason = "LOW_MOVEMENT";
        }
        return new Decision(significantMove, true, suspicious, reason);
    }

    public double computeFraudScore(CurrentPartnerLocationEntity previous, PartnerLocationUpdateRequest request, boolean suspiciousJump) {
        double score = 0.0d;
        if (Boolean.TRUE.equals(request.mockLocation)) {
            score += 40.0d;
        }
        if (Boolean.TRUE.equals(request.rootedDevice)) {
            score += 20.0d;
        }
        if (suspiciousJump) {
            score += 35.0d;
        }
        if (request.accuracyMeters != null && request.accuracyMeters > poorAccuracyMeters) {
            score += 10.0d;
        }
        return Math.min(100.0d, score);
    }

    private boolean isHeadingStable(BigDecimal previousHeading, Double newHeading) {
        if (previousHeading == null || newHeading == null) {
            return true;
        }
        double delta = Math.abs(previousHeading.doubleValue() - newHeading);
        double normalized = Math.min(delta, 360.0d - delta);
        return normalized <= headingDeltaThreshold;
    }

    private boolean isSuspiciousJump(CurrentPartnerLocationEntity previous, PartnerLocationUpdateRequest request, double movedMeters) {
        if (previous.getUpdatedAt() == null) {
            return false;
        }
        long dtMs = Math.max(1L, Duration.between(previous.getUpdatedAt(), LocalDateTime.now()).toMillis());
        double speedKmh = (movedMeters / dtMs) * 3_600.0d;
        return speedKmh > impossibleSpeedKmh;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public record Decision(boolean significantMovement, boolean acceptUpdate, boolean suspicious, String reason) {
    }
}
