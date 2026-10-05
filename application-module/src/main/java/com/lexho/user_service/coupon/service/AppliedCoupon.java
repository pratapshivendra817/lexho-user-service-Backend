package com.lexho.user_service.coupon.service;

import java.math.BigDecimal;

public record AppliedCoupon(
        Long couponId,
        String code,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
}
