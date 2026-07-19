package com.lexho.user_service.subscription.dto;

import java.time.LocalDateTime;

public record AdminVisitAssignmentResponse(
        Long id,
        Long visitId,
        Long partnerId,
        String partnerName,
        String status,
        String reason,
        LocalDateTime assignedAt
) {
}
