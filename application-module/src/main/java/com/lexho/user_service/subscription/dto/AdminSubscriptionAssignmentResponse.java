package com.lexho.user_service.subscription.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSubscriptionAssignmentResponse(
        Long subscriptionId,
        Long partnerId,
        String partnerName,
        String status,
        Long replacementPartnerId,
        String replacementPartnerName,
        String replacementReason,
        LocalDateTime assignedAt,
        LocalDateTime replacedAt,
        List<AdminVisitAssignmentResponse> visitAssignments
) {
}
