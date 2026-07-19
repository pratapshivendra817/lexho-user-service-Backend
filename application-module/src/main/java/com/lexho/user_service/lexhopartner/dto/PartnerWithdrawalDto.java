package com.lexho.user_service.lexhopartner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartnerWithdrawalDto(
        Long id,
        BigDecimal amount,
        String status,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
