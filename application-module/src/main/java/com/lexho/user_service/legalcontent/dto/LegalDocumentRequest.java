package com.lexho.user_service.legalcontent.dto;

import jakarta.validation.constraints.NotBlank;

public record LegalDocumentRequest(
        @NotBlank(message = "title is required") String title,
        String platform,
        @NotBlank(message = "documentType is required") String documentType,
        String format,
        String summary,
        @NotBlank(message = "content is required") String content,
        String changeNote,
        String effectiveFrom,
        Boolean publish,
        String userType) {

    public LegalDocumentRequest(String title,
                                String platform,
                                String documentType,
                                String format,
                                String summary,
                                String content,
                                String changeNote,
                                String effectiveFrom,
                                Boolean publish) {
        this(title, platform, documentType, format, summary, content, changeNote, effectiveFrom, publish, null);
    }

    public LegalDocumentRequest(String title,
                                String platform,
                                String documentType,
                                String format,
                                String summary,
                                String content,
                                String changeNote,
                                Boolean publish) {
        this(title, platform, documentType, format, summary, content, changeNote, null, publish, null);
    }
}
