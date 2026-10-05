package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PartnerSupportStatusUpdateRequest(
        @NotNull
        @Pattern(regexp = "OPEN|CLOSED", message = "status must be OPEN or CLOSED")
        String status
) {
}
