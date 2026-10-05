package com.lexho.user_service.coupon.dto;

import com.lexho.user_service.coupon.enums.CouponDiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        String title,
        String description,
        CouponDiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minimumPurchaseAmount,
        Integer totalUsageLimit,
        Integer perUserUsageLimit,
        Integer maxUsesPerDay,
        Integer usedCount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        boolean active,
        boolean customerVisible,
        boolean firstBookingOnly,
        Long applicableServiceId,
        Long applicableCategoryId,
        String applicableBookingType,
        Long assignedUserId,
        String sourceType,
        String sourceReferenceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
