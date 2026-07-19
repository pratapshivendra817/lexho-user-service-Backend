package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePartnerBankDetailsRequest(
        @NotBlank(message = "accountNumber is required")
        @Pattern(regexp = "^[0-9]{6,18}$", message = "accountNumber must contain 6 to 18 digits")
        String accountNumber,
        @NotBlank(message = "ifscCode is required")
        @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "ifscCode is invalid")
        String ifscCode
) {
}
