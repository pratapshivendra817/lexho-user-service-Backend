package com.lexho.user_service.customer.location.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LocationUpdateRequest {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonAlias({"lat"})
    @Schema(example = "28.6123")
    public Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonAlias({"lng", "lon"})
    @Schema(example = "77.2290")
    public Double longitude;

    @DecimalMin("0.0")
    @Schema(example = "25.5")
    public Double accuracyMeters;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Schema(example = "0.92")
    public Double confidenceScore;

    @DecimalMin("0")
    @Schema(example = "10")
    public Integer locationAgeSeconds;

    @Size(max = 32)
    @Schema(example = "GPS_AUTO")
    public String detectionSource;

    @Schema(example = "false")
    public Boolean mockLocation;

    @Schema(example = "false")
    public Boolean manualConfirmed;

    @Size(max = 128)
    @Schema(example = "7e5f0b0c-1fd4-4fd2-8d68-6f5e9d1b2c10")
    public String idempotencyKey;

    @DecimalMin("0")
    @JsonAlias({"timestamp", "eventTs", "clientTimestampMs"})
    @Schema(example = "1717133200000")
    public Long eventTimestampMs;
}
