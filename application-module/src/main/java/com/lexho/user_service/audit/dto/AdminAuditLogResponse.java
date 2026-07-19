package com.lexho.user_service.audit.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminAuditLogResponse(
        String id,
        String auditId,
        LocalDateTime date,
        LocalDateTime createdAt,
        LocalDateTime timestamp,
        String user,
        String adminName,
        String username,
        String role,
        String action,
        String entityType,
        String module,
        String entityId,
        String referenceId,
        String requestId,
        String correlationId,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        List<AdminAuditChangeResponse> changeHistory,
        List<AdminAuditChangeResponse> changes
) {
}
