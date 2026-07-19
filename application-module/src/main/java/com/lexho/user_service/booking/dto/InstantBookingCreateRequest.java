package com.lexho.user_service.booking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class InstantBookingCreateRequest {
    @NotNull
    @Positive
    public Long serviceId;

    public String addressId;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonAlias({"lat"})
    public Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonAlias({"lng", "lon"})
    public Double longitude;

    @Valid
    public Coordinates coordinates;

    @Size(max = 20)
    public String bookingType;

    public LocalDate date;

    @Size(max = 32)
    public String slot;

    @Size(max = 1000)
    public String note;

    public static class Coordinates {
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        public Double lat;

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        public Double lng;
    }
}
