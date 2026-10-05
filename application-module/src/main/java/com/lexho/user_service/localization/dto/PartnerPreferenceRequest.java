package com.lexho.user_service.localization.dto;

import jakarta.validation.constraints.NotBlank;

public record PartnerPreferenceRequest(@NotBlank(message = "language is required") String language) {
}
