package com.lexho.user_service.lexhopartner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartnerPayoutDto(
        Long id,
        Long bookingId,
        BigDecimal settlementAmount,
        BigDecimal reversedAmount,
        String currency,
        String status,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}
