package com.lexho.user_service.coupon.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String couponCode,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
}
