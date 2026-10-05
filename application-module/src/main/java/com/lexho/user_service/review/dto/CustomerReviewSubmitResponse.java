package com.lexho.user_service.review.dto;

import java.time.LocalDateTime;

public record CustomerReviewSubmitResponse(
        Long reviewId,
        Long bookingId,
        Integer rating,
        String reviewText,
        LocalDateTime createdAt,
        boolean reviewSubmitted
) {
}
