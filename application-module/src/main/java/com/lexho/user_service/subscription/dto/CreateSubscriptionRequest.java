package com.lexho.user_service.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateSubscriptionRequest(
        @NotNull(message = "serviceId is required")
        @Positive(message = "serviceId must be positive")
        Long serviceId,

        @NotBlank(message = "addressId is required")
        String addressId,

        Double latitude,

        Double longitude,

        String bookingMode,

        String serviceType,

        @NotEmpty(message = "frequencyDays cannot be empty")
        List<@NotBlank(message = "frequency day cannot be blank") String> frequencyDays,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @Positive(message = "visits must be positive")
        Integer visits,

        @DecimalMin(value = "0.01", message = "durationHours must be greater than 0")
        BigDecimal durationHours,

        com.lexho.user_service.subscription.enums.FamilySize familySize,

        @NotBlank(message = "timeSlot cannot be blank")
        String timeSlot,

        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,

        @jakarta.validation.constraints.Size(max = 40, message = "couponCode must be 40 characters or less")
        String couponCode
) {
    /** Backwards-compatible constructor for internal callers that still use legacy numeric IDs. */
    public CreateSubscriptionRequest(Long serviceId, Long addressId, String bookingMode, String serviceType,
                                     List<String> frequencyDays, LocalDate startDate, LocalDate endDate,
                                     Integer visits, BigDecimal durationHours,
                                     com.lexho.user_service.subscription.enums.FamilySize familySize,
                                     String timeSlot, BigDecimal price, String couponCode) {
        this(serviceId, addressId == null ? null : String.valueOf(addressId), null, null, bookingMode, serviceType,
                frequencyDays, startDate, endDate, visits, durationHours, familySize, timeSlot, price, couponCode);
    }
}
