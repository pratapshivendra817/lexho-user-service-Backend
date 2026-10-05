package com.lexho.user_service.referral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReferralApplyRequest(
        @NotBlank(message = "code is required")
        @Size(max = 40, message = "code must be 40 characters or less")
        String code
) {
}
