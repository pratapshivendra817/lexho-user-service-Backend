package com.lexho.user_service.dispatch.service;

import com.lexho.partner.entity.Partner;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
public class DispatchScoringPolicy {

    private final double dispatchRadiusKm;
    private final double weightDistance;
    private final double weightPincodeMatch;
    private final double weightActiveJobs;
    private final double weightRating;

    public DispatchScoringPolicy(
            @Value("${app.dispatch.radius-km:15}") double dispatchRadiusKm,
            @Value("${app.dispatch.score.weight.distance:0.45}") double weightDistance,
            @Value("${app.dispatch.score.weight.pincode-match:0.25}") double weightPincodeMatch,
            @Value("${app.dispatch.score.weight.active-jobs:0.15}") double weightActiveJobs,
            @Value("${app.dispatch.score.weight.rating:0.15}") double weightRating) {
        this.dispatchRadiusKm = dispatchRadiusKm;
        this.weightDistance = weightDistance;
        this.weightPincodeMatch = weightPincodeMatch;
        this.weightActiveJobs = weightActiveJobs;
        this.weightRating = weightRating;
    }

    public boolean isWithinDispatchRadius(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return true;
        }
        return distanceKm.doubleValue() <= dispatchRadiusKm;
    }

    public Comparator<DispatchOrchestrationService.Candidate> comparator(String targetPincode) {
        return Comparator
                .comparing((DispatchOrchestrationService.Candidate c) -> -score(c, targetPincode))
                .thenComparing(c -> c.distance() == null ? BigDecimal.valueOf(999999) : c.distance());
    }

    public double getDispatchRadiusKm() {
        return dispatchRadiusKm;
    }

    private double score(DispatchOrchestrationService.Candidate c, String targetPincode) {
        double distanceScore;
        if (c.distance() == null) {
            distanceScore = 0.1;
        } else {
            double km = c.distance().doubleValue();
            distanceScore = 1.0 - Math.min(1.0, km / Math.max(0.1, dispatchRadiusKm));
        }

        double pincodeScore = targetPincode.equals(c.shift().getCurrentPincode()) ? 1.0 : 0.0;
        int activeJobs = c.shift().getActiveJobsCount();
        int maxActiveJobs = c.shift().getMaxActiveJobs() <= 0 ? 1 : c.shift().getMaxActiveJobs();
        double jobsScore = 1.0 - Math.min(1.0, ((double) activeJobs) / maxActiveJobs);
        double rating = c.partner().getRating() == null ? 0.0 : c.partner().getRating();
        double ratingScore = Math.max(0.0, Math.min(1.0, rating / 5.0));

        return (weightDistance * distanceScore)
                + (weightPincodeMatch * pincodeScore)
                + (weightActiveJobs * jobsScore)
                + (weightRating * ratingScore);
    }
}
