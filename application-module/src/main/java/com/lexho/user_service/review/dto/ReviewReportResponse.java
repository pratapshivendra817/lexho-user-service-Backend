package com.lexho.user_service.review.dto;

import com.lexho.user_service.review.enums.ReviewReportStatus;

import java.time.LocalDateTime;

public record ReviewReportResponse(
        Long reportId,
        Long reviewId,
        Long bookingId,
        Long reporterId,
        String reporterName,
        String reason,
        String reviewContent,
        ReviewReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        Long resolvedByAdminId,
        String resolutionNote
) {
}
