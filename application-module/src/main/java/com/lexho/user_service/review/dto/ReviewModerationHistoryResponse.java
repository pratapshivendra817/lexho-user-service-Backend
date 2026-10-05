package com.lexho.user_service.review.dto;

import com.lexho.user_service.review.enums.ReviewStatus;

import java.time.LocalDateTime;

public record ReviewModerationHistoryResponse(
        Long id,
        Long reviewId,
        ReviewStatus previousStatus,
        ReviewStatus newStatus,
        String action,
        Long adminId,
        String adminName,
        String adminEmail,
        String reason,
        LocalDateTime createdAt
) {
}
