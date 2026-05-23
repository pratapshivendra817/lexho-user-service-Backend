package com.lexho.user_service.lexhopartner.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationDto(
        Long id,
        String title,
        String body,
        boolean isRead,
        LocalDateTime createdAt
) {
}
