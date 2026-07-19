package com.lexho.user_service.audit.dto;

import java.time.LocalDateTime;

public record AdminAuditChangeResponse(
        String id,
        String field,
        String oldValue,
        String newValue,
        LocalDateTime changedAt
) {
}
