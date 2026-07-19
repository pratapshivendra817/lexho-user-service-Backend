package com.lexho.user_service.review.dto;

import java.time.LocalDateTime;

public record CustomerReviewResponse(
        Long reviewId,
        Integer rating,
        String reviewText,
        LocalDateTime createdAt
) {
}
