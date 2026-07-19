package com.lexho.user_service.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class BookingRescheduleRequest {
    @NotNull
    public OffsetDateTime date;
}
