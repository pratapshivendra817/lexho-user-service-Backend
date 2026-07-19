package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;

public record SubscriptionDurationPriceResponse(
        Long id,
        Long serviceId,
        BigDecimal durationHours,
        BigDecimal perVisitPrice,
        boolean active
) {
}
