package com.lexho.user_service.review.dto;

public record RatingDistributionResponse(
        int rating,
        long count
) {
}
