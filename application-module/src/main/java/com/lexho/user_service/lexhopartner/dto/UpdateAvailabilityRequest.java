package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "online flag is required")
        Boolean online
) {
}
