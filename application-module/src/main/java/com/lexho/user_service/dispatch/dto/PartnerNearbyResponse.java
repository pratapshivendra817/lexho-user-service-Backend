package com.lexho.user_service.dispatch.dto;

import java.util.List;

public class PartnerNearbyResponse {
    public List<PartnerSummary> partners;

    public static class PartnerSummary {
        public Long partnerId;
        public Double distanceKm;
        public Double rating;
        public Integer activeJobs;
        public Integer maxActiveJobs;
        public Boolean online;
        public String serviceCategory;
        public Double lat;
        public Double lng;
        public String geohash;
        public String lastSeenAt;
    }
}

