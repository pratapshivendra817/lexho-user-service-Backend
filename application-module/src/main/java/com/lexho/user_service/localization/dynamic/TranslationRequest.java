package com.lexho.user_service.localization.dynamic;

public record TranslationRequest(
        String sourceLocale,
        String targetLocale,
        String text,
        String entityType,
        String fieldName
) {
}
