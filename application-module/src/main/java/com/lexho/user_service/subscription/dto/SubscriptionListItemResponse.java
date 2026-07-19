package com.lexho.user_service.subscription.dto;

import java.time.LocalDate;

public record SubscriptionListItemResponse(
        Long subscriptionId,
        String serviceName,
        LocalDate startDate,
        LocalDate endDate,
        Integer visits,
        Long completedVisits,
        Long remainingVisits,
        String status
) {
}
