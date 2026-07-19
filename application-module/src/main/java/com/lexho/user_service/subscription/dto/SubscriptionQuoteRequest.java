package com.lexho.user_service.subscription.dto;

import com.lexho.user_service.subscription.enums.FamilySize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionQuoteRequest(
        @NotNull(message = "serviceId is required")
        @Positive(message = "serviceId must be positive")
        Long serviceId,

        @NotBlank(message = "serviceType is required")
        String serviceType,

        FamilySize familySize,

        BigDecimal durationHours,

        @NotEmpty(message = "frequencyDays cannot be empty")
        List<@NotBlank(message = "frequency day cannot be blank") String> frequencyDays,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate
) {
}
