package com.lexho.user_service.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServiceRatingSummary {

    private Long serviceId;
    private String serviceName;
    private Double averageRating;
    private Long totalReviews;
}
