package com.lexho.user_service.referral.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReferralConfigurationRequest(
        @NotNull(message = "active is required")
        Boolean active,

        @NotNull(message = "referrerRewardAmount is required")
        @DecimalMin(value = "0.00", message = "referrerRewardAmount must be zero or greater")
        BigDecimal referrerRewardAmount,

        @NotNull(message = "referredUserRewardAmount is required")
        @DecimalMin(value = "0.00", message = "referredUserRewardAmount must be zero or greater")
        BigDecimal referredUserRewardAmount,

        @NotNull(message = "minimumQualifyingPurchaseAmount is required")
        @DecimalMin(value = "0.00", message = "minimumQualifyingPurchaseAmount must be zero or greater")
        BigDecimal minimumQualifyingPurchaseAmount,

        @NotBlank(message = "qualificationCondition is required")
        @Size(max = 40, message = "qualificationCondition must be 40 characters or less")
        String qualificationCondition,

        @Positive(message = "expiryDays must be positive")
        Integer expiryDays
) {
}
