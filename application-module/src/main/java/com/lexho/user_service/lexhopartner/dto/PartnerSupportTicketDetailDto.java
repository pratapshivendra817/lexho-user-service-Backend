package com.lexho.user_service.lexhopartner.dto;

import java.util.List;

public record PartnerSupportTicketDetailDto(
        String id,
        String subject,
        String status,
        String categoryId,
        String bookingId,
        String priority,
        List<String> attachmentUrls,
        String createdAt,
        String updatedAt,
        List<PartnerSupportMessageDto> messages
) {
}
