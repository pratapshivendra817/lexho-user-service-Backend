package com.lexho.user_service.legalcontent.dto;

import java.time.OffsetDateTime;

public record LegalDocumentResponse(
        String id,
        String slug,
        String title,
        String platform,
        String documentType,
        String status,
        String format,
        String summary,
        String content,
        int version,
        String createdBy,
        String updatedBy,
        String publishedBy,
        OffsetDateTime effectiveFrom,
        OffsetDateTime updatedAt,
        OffsetDateTime publishedAt) {
}
