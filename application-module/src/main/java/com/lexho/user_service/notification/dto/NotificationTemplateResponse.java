package com.lexho.user_service.notification.dto;

import com.lexho.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationTemplateResponse(
        Long id,
        String templateName,
        NotificationType notificationType,
        String titleTemplate,
        String messageTemplate,
        String language,
        boolean active,
        LocalDateTime createdAt
) {
}
