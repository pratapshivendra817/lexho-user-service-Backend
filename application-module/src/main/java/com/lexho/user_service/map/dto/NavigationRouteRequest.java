package com.lexho.user_service.map.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record NavigationRouteRequest(
        @NotNull(message = "partnerLatitude is required")
        @DecimalMin(value = "-90.0", message = "partnerLatitude must be >= -90")
        @DecimalMax(value = "90.0", message = "partnerLatitude must be <= 90")
        Double partnerLatitude,
        @NotNull(message = "partnerLongitude is required")
        @DecimalMin(value = "-180.0", message = "partnerLongitude must be >= -180")
        @DecimalMax(value = "180.0", message = "partnerLongitude must be <= 180")
        Double partnerLongitude,
        @NotNull(message = "customerLatitude is required")
        @DecimalMin(value = "-90.0", message = "customerLatitude must be >= -90")
        @DecimalMax(value = "90.0", message = "customerLatitude must be <= 90")
        Double customerLatitude,
        @NotNull(message = "customerLongitude is required")
        @DecimalMin(value = "-180.0", message = "customerLongitude must be >= -180")
        @DecimalMax(value = "180.0", message = "customerLongitude must be <= 180")
        Double customerLongitude
) {
}
