package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionCreateResponse(
        Long id,
        Long subscriptionId,
        Long bookingId,
        Long serviceId,
        Long addressId,
        String bookingMode,
        String serviceType,
        List<String> frequencyDays,
        LocalDate startDate,
        LocalDate endDate,
        Integer visits,
        BigDecimal durationHours,
        String familySize,
        String familySizeLabel,
        BigDecimal perVisitPrice,
        String timeSlot,
        BigDecimal price,
        String status,
        List<SubscriptionCalendarItemResponse> calendar
) {
}
