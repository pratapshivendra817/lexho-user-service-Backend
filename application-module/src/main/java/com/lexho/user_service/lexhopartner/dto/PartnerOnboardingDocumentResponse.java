package com.lexho.user_service.lexhopartner.dto;

public record PartnerOnboardingDocumentResponse(
        String type,
        String key,
        String label,
        boolean required,
        boolean uploaded,
        String url,
        String previewUrl,
        String downloadUrl
) {
}
