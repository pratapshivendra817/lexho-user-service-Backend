package com.lexho.user_service.emergency.dto;

import java.math.BigDecimal;

public record EmergencyIncidentLocationView(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        String address
) {
}
