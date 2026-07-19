package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PartnerNearbyRequest {
    @NotNull(message = "lat is required")
    @DecimalMin(value = "-90.0", message = "lat must be >= -90")
    @DecimalMax(value = "90.0", message = "lat must be <= 90")
    public Double lat;

    @NotNull(message = "lng is required")
    @DecimalMin(value = "-180.0", message = "lng must be >= -180")
    @DecimalMax(value = "180.0", message = "lng must be <= 180")
    public Double lng;

    @NotBlank(message = "serviceType is required")
    public String serviceType;

    @NotNull(message = "radiusKm is required")
    @DecimalMin(value = "0.2", message = "radiusKm must be >= 0.2")
    @DecimalMax(value = "30.0", message = "radiusKm must be <= 30")
    public Double radiusKm;
}

