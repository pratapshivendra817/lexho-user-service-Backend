package com.lexho.user_service.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewStatsResponse {

    private long totalReviews;
    private double averageRating;
    private long totalReportedReviews;
    private long hiddenReviews;
    private List<ProviderRatingSummary> providerWiseRatings;
    private List<ServiceRatingSummary> serviceWiseRatings;
}
