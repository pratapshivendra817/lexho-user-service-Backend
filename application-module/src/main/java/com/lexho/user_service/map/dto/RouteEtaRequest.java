package com.lexho.user_service.map.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class RouteEtaRequest {

    @NotNull(message = "originLatitude is required")
    @DecimalMin(value = "-90.0", message = "originLatitude must be >= -90")
    @DecimalMax(value = "90.0", message = "originLatitude must be <= 90")
    public Double originLatitude;

    @NotNull(message = "originLongitude is required")
    @DecimalMin(value = "-180.0", message = "originLongitude must be >= -180")
    @DecimalMax(value = "180.0", message = "originLongitude must be <= 180")
    public Double originLongitude;

    @NotNull(message = "destinationLatitude is required")
    @DecimalMin(value = "-90.0", message = "destinationLatitude must be >= -90")
    @DecimalMax(value = "90.0", message = "destinationLatitude must be <= 90")
    public Double destinationLatitude;

    @NotNull(message = "destinationLongitude is required")
    @DecimalMin(value = "-180.0", message = "destinationLongitude must be >= -180")
    @DecimalMax(value = "180.0", message = "destinationLongitude must be <= 180")
    public Double destinationLongitude;

    @Pattern(regexp = "driving|walking|cycling", message = "profile must be driving, walking or cycling")
    public String profile = "driving";
}

