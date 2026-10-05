package com.lexho.user_service.lexhopartner.dto;

import java.util.List;

public record PartnerSupportTicketDto(
        String id,
        String subject,
        String message,
        String status,
        String categoryId,
        String bookingId,
        String priority,
        List<String> attachmentUrls,
        String createdAt,
        String updatedAt
) {
}
