package com.lexho.user_service.emergency.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmergencyLocationResponse(
        Long id,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        String address,
        String source,
        LocalDateTime capturedAt
) {
}
