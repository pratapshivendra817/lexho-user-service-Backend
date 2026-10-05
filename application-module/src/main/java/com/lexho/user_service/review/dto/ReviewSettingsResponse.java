package com.lexho.user_service.review.dto;

import java.util.List;

public record ReviewSettingsResponse(
        boolean reviewsEnabled,
        boolean ratingEnabled,
        boolean writtenReviewsEnabled,
        boolean autoApproveReviews,
        List<ServiceReviewSettingResponse> services
) {
    public record ServiceReviewSettingResponse(
            Long serviceId,
            String serviceName,
            Long categoryId,
            String categoryName,
            boolean reviewEnabled
    ) {
    }
}
