package com.lexho.user_service.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscriptionDurationPriceRequest(
        @Positive(message = "serviceId must be positive")
        Long serviceId,

        @NotNull(message = "durationHours is required")
        @DecimalMin(value = "0.01", message = "durationHours must be greater than 0")
        BigDecimal durationHours,

        @NotNull(message = "perVisitPrice is required")
        @DecimalMin(value = "0.01", message = "perVisitPrice must be greater than 0")
        BigDecimal perVisitPrice,

        Boolean active
) {
}
