package com.lexho.user_service.localization.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslationUpsertRequest(
        @NotBlank(message = "key is required") String key,
        @NotBlank(message = "value is required") String value) {
}
