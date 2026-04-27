package com.lexho.user_service.banner.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BannerRedeemRequest {
    private Long userId;
    private Long bookingId;
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount applied cannot be negative")
    private BigDecimal discountApplied;
}

