package com.lexho.user_service.review.dto;

import java.util.List;
import java.util.Map;

public record ReviewDetailResponse(
        ReviewResponse review,
        Map<String, Object> booking,
        Map<String, Object> customer,
        Map<String, Object> partner,
        Map<String, Object> service,
        List<ReviewReportResponse> reports,
        List<ReviewModerationHistoryResponse> moderationHistory
) {
}
