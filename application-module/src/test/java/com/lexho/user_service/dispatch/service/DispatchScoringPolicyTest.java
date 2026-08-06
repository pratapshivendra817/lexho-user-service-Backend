package com.lexho.user_service.dispatch.service;

import com.lexho.provider.entity.Partner;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DispatchScoringPolicyTest {
    private final DispatchScoringPolicy policy = new DispatchScoringPolicy(dispatchProperties(10), 0.45, 0.25, 0.15, 0.15);

    @Test
    void checksDispatchRadiusIncludingUnknownDistance() {
        assertFalse(policy.isWithinDispatchRadius(null, 10));
        assertTrue(policy.isWithinDispatchRadius(BigDecimal.TEN, 10));
        assertFalse(policy.isWithinDispatchRadius(BigDecimal.valueOf(10.01), 10));
        assertEquals(10.0, policy.getDispatchRadiusKm());
    }

    @Test
    void comparatorPrefersNearbyPincodeMatchedAvailablePartner() {
        var preferred = candidate("560001", 0, 3, 4.8, BigDecimal.ONE);
        var weaker = candidate("560002", 3, 3, null, BigDecimal.valueOf(8));
        List<DispatchOrchestrationService.Candidate> candidates = new ArrayList<>(List.of(weaker, preferred));

        candidates.sort(policy.comparator("560001"));

        assertSame(preferred, candidates.get(0));
    }

    @Test
    void comparatorHandlesNullDistanceAndNonPositiveCapacity() {
        var first = candidate("560001", 1, 0, 5.0, null);
        var second = candidate("560001", 1, 0, 5.0, BigDecimal.valueOf(999));
        assertDoesNotThrow(() -> policy.comparator("560001").compare(first, second));
    }

    private DispatchOrchestrationService.Candidate candidate(String pincode, int jobs, int maxJobs,
                                                               Double rating, BigDecimal distance) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setCurrentPincode(pincode); shift.setActiveJobsCount(jobs); shift.setMaxActiveJobs(maxJobs);
        Partner partner = new Partner(); partner.setRating(rating);
        return new DispatchOrchestrationService.Candidate(shift, partner, distance, 10);
    }

    private static DispatchProperties dispatchProperties(double maxRadiusKm) {
        DispatchProperties properties = new DispatchProperties();
        properties.getPartner().setMaxSearchRadiusKm(maxRadiusKm);
        properties.getPartner().setInitialSearchRadiusKm(Math.min(1.0d, maxRadiusKm));
        return properties;
    }
}
