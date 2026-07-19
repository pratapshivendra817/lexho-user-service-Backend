package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionDetailResponse(
        Long subscriptionId,
        Long serviceId,
        String serviceName,
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
        Long completedVisits,
        Long remainingVisits,
        List<SubscriptionCalendarItemResponse> calendar
) {
}
