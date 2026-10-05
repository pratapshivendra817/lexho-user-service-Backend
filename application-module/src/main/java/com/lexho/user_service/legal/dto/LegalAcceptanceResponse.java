package com.lexho.user_service.legal.dto;

import java.time.LocalDateTime;

public record LegalAcceptanceResponse(
        Long id,
        Long userId,
        String userType,
        String documentType,
        int documentVersion,
        LocalDateTime acceptedAt) {
}
