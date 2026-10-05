package com.lexho.user_service.lexhopartner.dto;

import java.util.List;

public record PartnerSupportMessageDto(
        String id,
        String sender,
        String message,
        List<String> attachmentUrls,
        String createdAt
) {
}
