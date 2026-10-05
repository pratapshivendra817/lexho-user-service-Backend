package com.lexho.user_service.localization.dto;

import jakarta.validation.constraints.NotNull;

public record LanguageUpdateRequest(@NotNull(message = "enabled is required") Boolean enabled) {
}
