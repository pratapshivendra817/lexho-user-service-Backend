package com.lexho.user_service.office.dto;

import java.math.BigDecimal;

public record ScreeningCentrePartnerResponse(
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone) {
}
