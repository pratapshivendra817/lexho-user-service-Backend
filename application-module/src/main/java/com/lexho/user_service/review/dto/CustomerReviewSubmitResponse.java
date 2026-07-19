package com.lexho.user_service.review.dto;

public record CustomerReviewSubmitResponse(
        Long reviewId,
        Long bookingId,
        Integer rating,
        boolean reviewSubmitted
) {
}
