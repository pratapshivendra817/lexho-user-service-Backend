package com.lexho.user_service.payment.dto;

import java.util.List;

public record AdminWebhookEventListResponse(
        List<AdminWebhookEventResponse> content
) {
}
