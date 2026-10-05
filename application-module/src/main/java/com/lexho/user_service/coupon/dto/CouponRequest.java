package com.lexho.user_service.coupon.dto;

import com.lexho.user_service.coupon.enums.CouponDiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank(message = "code is required")
        @Size(max = 40, message = "code must be 40 characters or less")
        String code,

        @Size(max = 120, message = "title must be 120 characters or less")
        String title,

        @Size(max = 1000, message = "description must be 1000 characters or less")
        String description,

        @NotNull(message = "discountType is required")
        CouponDiscountType discountType,

        @NotNull(message = "discountValue is required")
        @DecimalMin(value = "0.01", message = "discountValue must be greater than 0")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "maxDiscountAmount must be zero or greater")
        BigDecimal maxDiscountAmount,

        @DecimalMin(value = "0.00", message = "minimumPurchaseAmount must be zero or greater")
        BigDecimal minimumPurchaseAmount,

        @Positive(message = "totalUsageLimit must be positive")
        Integer totalUsageLimit,

        @Positive(message = "perUserUsageLimit must be positive")
        Integer perUserUsageLimit,

        @Positive(message = "maxUsesPerDay must be positive")
        Integer maxUsesPerDay,

        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Boolean active,
        Boolean customerVisible,
        Boolean firstBookingOnly,
        Long applicableServiceId,
        Long applicableCategoryId,
        @Size(max = 30, message = "applicableBookingType must be 30 characters or less")
        String applicableBookingType
) {
}
