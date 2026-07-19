package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;

public record SubscriptionQuoteResponse(
        Long serviceId,
        String serviceType,
        String familySize,
        String familySizeLabel,
        BigDecimal durationHours,
        BigDecimal pricePerVisit,
        Integer visits,
        BigDecimal totalPrice,
        String currency
) {
}
