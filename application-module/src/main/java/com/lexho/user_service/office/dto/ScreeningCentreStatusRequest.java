package com.lexho.user_service.office.dto;

import jakarta.validation.constraints.NotNull;

public record ScreeningCentreStatusRequest(@NotNull(message = "active is required") Boolean active) {
}
