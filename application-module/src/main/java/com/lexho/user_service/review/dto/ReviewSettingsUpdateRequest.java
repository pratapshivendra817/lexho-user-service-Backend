package com.lexho.user_service.review.dto;

import java.util.List;

public class ReviewSettingsUpdateRequest {
    public Boolean reviewsEnabled;
    public Boolean ratingEnabled;
    public Boolean writtenReviewsEnabled;
    public Boolean autoApproveReviews;
    public List<ServiceReviewSettingUpdate> services;

    public static class ServiceReviewSettingUpdate {
        public Long serviceId;
        public Boolean reviewEnabled;
    }
}
