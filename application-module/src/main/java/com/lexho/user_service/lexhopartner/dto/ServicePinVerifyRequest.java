package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ServicePinVerifyRequest(
        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^\\d{4}$", message = "PIN must be 4 digits")
        String pin
) {
}
