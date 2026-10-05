package com.lexho.user_service.review.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ReviewStatsResponse {

    private long totalReviews;
    private long pendingReviews;
    private long approvedReviews;
    private long rejectedReviews;
    private long hiddenReviews;
    private double averageRating;
    private long totalReportedReviews;
    private List<RatingDistributionResponse> ratingDistribution;
    private List<ProviderRatingSummary> providerWiseRatings;
    private List<ServiceRatingSummary> serviceWiseRatings;

    public ReviewStatsResponse(long totalReviews,
                               double averageRating,
                               long totalReportedReviews,
                               long hiddenReviews,
                               List<ProviderRatingSummary> providerWiseRatings,
                               List<ServiceRatingSummary> serviceWiseRatings) {
        this(totalReviews, 0, 0, 0, hiddenReviews, averageRating, totalReportedReviews,
                List.of(), providerWiseRatings, serviceWiseRatings);
    }

    public ReviewStatsResponse(long totalReviews,
                               long pendingReviews,
                               long approvedReviews,
                               long rejectedReviews,
                               long hiddenReviews,
                               double averageRating,
                               long totalReportedReviews,
                               List<RatingDistributionResponse> ratingDistribution,
                               List<ProviderRatingSummary> providerWiseRatings,
                               List<ServiceRatingSummary> serviceWiseRatings) {
        this.totalReviews = totalReviews;
        this.pendingReviews = pendingReviews;
        this.approvedReviews = approvedReviews;
        this.rejectedReviews = rejectedReviews;
        this.hiddenReviews = hiddenReviews;
        this.averageRating = averageRating;
        this.totalReportedReviews = totalReportedReviews;
        this.ratingDistribution = ratingDistribution;
        this.providerWiseRatings = providerWiseRatings;
        this.serviceWiseRatings = serviceWiseRatings;
    }
}
