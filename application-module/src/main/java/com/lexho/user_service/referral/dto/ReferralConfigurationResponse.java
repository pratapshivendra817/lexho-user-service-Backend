package com.lexho.user_service.referral.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReferralConfigurationResponse(
        boolean active,
        BigDecimal referrerRewardAmount,
        BigDecimal referredUserRewardAmount,
        BigDecimal minimumQualifyingPurchaseAmount,
        String qualificationCondition,
        Integer expiryDays,
        LocalDateTime updatedAt,
        Long updatedBy
) {
}
