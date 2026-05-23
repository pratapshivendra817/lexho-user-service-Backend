package com.lexho.user_service.lexhopartner.dto;

import java.time.LocalDateTime;

public record PartnerEarningTransactionDto(
        Long orderId,
        String customerName,
        String serviceType,
        Double grossAmount,
        Double settlementAmount,
        String settlementStatus,
        LocalDateTime completedAt
) {
}

