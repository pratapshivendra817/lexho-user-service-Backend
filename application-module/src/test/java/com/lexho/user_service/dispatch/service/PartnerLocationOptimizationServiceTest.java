package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.dispatch.dto.PartnerLocationUpdateRequest;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerLocationOptimizationServiceTest {

    private final PartnerLocationOptimizationService service =
            new PartnerLocationOptimizationService(20.0, 80.0, 2.5, 7.0, 300.0);

    @Test
    void evaluateShouldRejectPoorAccuracy() {
        PartnerLocationUpdateRequest request = request(28.1, 77.1);
        request.accuracyMeters = 120.0;

        PartnerLocationOptimizationService.Decision decision = service.evaluate(null, request);

        assertFalse(decision.significantMovement());
        assertTrue(decision.acceptUpdate());
        assertTrue(decision.suspicious());
        assertEquals("POOR_ACCURACY", decision.reason());
    }

    @Test
    void evaluateShouldAcceptFirstLocationAsSignificantMovement() {
        PartnerLocationUpdateRequest request = request(28.1, 77.1);

        PartnerLocationOptimizationService.Decision decision = service.evaluate(null, request);

        assertTrue(decision.significantMovement());
        assertTrue(decision.acceptUpdate());
        assertFalse(decision.suspicious());
    }

    @Test
    void evaluateShouldClassifyStationaryLowSpeedUpdateAsHeartbeat() {
        CurrentPartnerLocationEntity previous = previous(28.1000000, 77.1000000, 90.0, LocalDateTime.now().minusMinutes(10));
        PartnerLocationUpdateRequest request = request(28.1000100, 77.1000100);
        request.speedKmh = 1.0;
        request.headingDegrees = 92.0;

        PartnerLocationOptimizationService.Decision decision = service.evaluate(previous, request);

        assertFalse(decision.significantMovement());
        assertEquals("LOW_MOVEMENT", decision.reason());
    }

    @Test
    void evaluateShouldMarkImpossibleJumpSuspicious() {
        CurrentPartnerLocationEntity previous = previous(28.1000000, 77.1000000, 90.0, LocalDateTime.now().minusSeconds(1));
        PartnerLocationUpdateRequest request = request(29.5000000, 78.5000000);
        request.speedKmh = 50.0;
        request.headingDegrees = 180.0;

        PartnerLocationOptimizationService.Decision decision = service.evaluate(previous, request);

        assertTrue(decision.significantMovement());
        assertTrue(decision.suspicious());
        assertEquals("IMPOSSIBLE_SPEED", decision.reason());
    }

    @Test
    void computeFraudScoreShouldCapAtOneHundred() {
        PartnerLocationUpdateRequest request = request(28.1, 77.1);
        request.mockLocation = true;
        request.rootedDevice = true;
        request.accuracyMeters = 120.0;

        assertEquals(100.0, service.computeFraudScore(null, request, true));
    }

    private PartnerLocationUpdateRequest request(double latitude, double longitude) {
        PartnerLocationUpdateRequest request = new PartnerLocationUpdateRequest();
        request.latitude = latitude;
        request.longitude = longitude;
        request.accuracyMeters = 10.0;
        return request;
    }

    private CurrentPartnerLocationEntity previous(double latitude, double longitude, double heading,
                                                  LocalDateTime updatedAt) {
        CurrentPartnerLocationEntity previous = new CurrentPartnerLocationEntity();
        previous.setLatitude(BigDecimal.valueOf(latitude));
        previous.setLongitude(BigDecimal.valueOf(longitude));
        previous.setHeadingDegrees(BigDecimal.valueOf(heading));
        previous.onCreate();
        ReflectionTestUtils.setField(previous, "updatedAt", updatedAt);
        return previous;
    }
}
