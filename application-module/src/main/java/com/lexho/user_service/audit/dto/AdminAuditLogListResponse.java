package com.lexho.user_service.audit.dto;

import java.util.List;

public record AdminAuditLogListResponse(
        List<AdminAuditLogResponse> content
) {
}
