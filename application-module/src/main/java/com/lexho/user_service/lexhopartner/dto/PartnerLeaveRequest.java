package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PartnerLeaveRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        String reason
) {
}
