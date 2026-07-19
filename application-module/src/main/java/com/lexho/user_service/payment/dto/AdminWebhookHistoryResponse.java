package com.lexho.user_service.payment.dto;

import java.time.LocalDateTime;

public record AdminWebhookHistoryResponse(
        String id,
        String status,
        String message,
        String errorMessage,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}
