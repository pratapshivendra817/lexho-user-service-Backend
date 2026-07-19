package com.lexho.user_service.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPartnerAssignRequest(
        @NotNull(message = "partnerId is required")
        Long partnerId,

        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
