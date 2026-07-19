package com.lexho.payment.dto;

public record WebhookProcessResult(
        String eventId,
        String eventType,
        String status,
        boolean processed,
        boolean duplicate,
        boolean retryScheduled,
        String message
) {
    public static WebhookProcessResult processed(String eventId, String eventType) {
        return new WebhookProcessResult(eventId, eventType, "SUCCESS", true, false, false, "Webhook processed");
    }

    public static WebhookProcessResult duplicate(String eventId, String eventType) {
        return new WebhookProcessResult(eventId, eventType, "DUPLICATE", false, true, false, "Duplicate webhook ignored");
    }

    public static WebhookProcessResult retryScheduled(String eventId, String eventType) {
        return new WebhookProcessResult(eventId, eventType, "RETRY_SCHEDULED", false, false, true, "Webhook accepted for retry");
    }
}
