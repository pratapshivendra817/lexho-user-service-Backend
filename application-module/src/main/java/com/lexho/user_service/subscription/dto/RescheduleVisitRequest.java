package com.lexho.user_service.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RescheduleVisitRequest(
        @NotNull(message = "newDate is required")
        LocalDate newDate
) {
}
