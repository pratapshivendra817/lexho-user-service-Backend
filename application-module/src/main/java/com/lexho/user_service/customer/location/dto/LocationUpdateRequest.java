package com.lexho.user_service.customer.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class LocationUpdateRequest {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Schema(example = "28.6123")
    public Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Schema(example = "77.2290")
    public Double longitude;
}
