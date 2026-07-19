package com.lexho.user_service.subscription.dto;

import java.math.BigDecimal;

public record CookSubscriptionPricingResponse(
        Long id,
        Long serviceId,
        String familySize,
        String familySizeLabel,
        BigDecimal pricePerVisit,
        boolean active
) {
}
