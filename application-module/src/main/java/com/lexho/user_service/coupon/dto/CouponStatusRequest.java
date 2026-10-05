package com.lexho.user_service.coupon.dto;

import jakarta.validation.constraints.NotNull;

public record CouponStatusRequest(@NotNull(message = "active is required") Boolean active) {
}
