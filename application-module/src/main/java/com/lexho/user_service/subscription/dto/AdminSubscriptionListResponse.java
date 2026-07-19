package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminSubscriptionListResponse(
        Long subscriptionId,
        Long customerId,
        Long serviceId,
        String serviceName,
        Long addressId,
        String bookingMode,
        String serviceType,
        LocalDate startDate,
        LocalDate endDate,
        Integer visits,
        Long completedVisits,
        Long remainingVisits,
        BigDecimal durationHours,
        String familySize,
        String familySizeLabel,
        BigDecimal perVisitPrice,
        String timeSlot,
        BigDecimal price,
        String status
) {
}
