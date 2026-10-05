package com.lexho.user_service.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CouponValidationRequest(
        @NotBlank(message = "code is required")
        @Size(max = 40, message = "code must be 40 characters or less")
        String code,

        Long planId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        Long categoryId,

        String bookingType
) {
}
