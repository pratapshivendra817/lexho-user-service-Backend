package com.lexho.user_service.office.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ScreeningCentreAdminResponse(
        Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String type,
        boolean active,
        String mapProvider,
        String placeId,
        String landmark,
        String entranceInfo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
