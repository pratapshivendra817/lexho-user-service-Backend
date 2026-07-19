package com.lexho.user_service.subscription.dto;

import com.lexho.user_service.subscription.enums.FamilySize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CookSubscriptionPricingRequest(
        @Positive(message = "serviceId must be positive")
        Long serviceId,

        @NotNull(message = "familySize is required")
        FamilySize familySize,

        @NotNull(message = "pricePerVisit is required")
        @DecimalMin(value = "0.01", message = "pricePerVisit must be greater than 0")
        BigDecimal pricePerVisit,

        Boolean active
) {
}
