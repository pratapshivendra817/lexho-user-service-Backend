package com.lexho.user_service.payment.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminWebhookEventResponse(
        String id,
        String eventId,
        String webhookEventId,
        String eventType,
        String type,
        String paymentId,
        String razorpayPaymentId,
        String status,
        String processingStatus,
        LocalDateTime processedAt,
        LocalDateTime updatedAt,
        int retryCount,
        int retries,
        String requestId,
        String correlationId,
        Map<String, Object> payload,
        Map<String, Object> eventPayload,
        String errorMessage,
        String error,
        String retryStatus,
        List<AdminWebhookHistoryResponse> history,
        List<AdminWebhookHistoryResponse> processingHistory
) {
}
