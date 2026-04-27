package com.lexho.user_service.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProviderRatingSummary {

    private Long providerId;
    private String providerName;
    private Double averageRating;
    private Long totalReviews;
}
