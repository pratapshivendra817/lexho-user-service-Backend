package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class PartnerLocationUpdateRequest {
    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    public Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    public Double longitude;

    public String source;
}
