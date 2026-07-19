package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;

public record SubscriptionPriceQuoteResponse(
        Long serviceId,
        BigDecimal durationHours,
        BigDecimal perVisitPrice,
        Integer visits,
        BigDecimal totalPrice
) {
}
