package com.lexho.user_service.customer.location.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

public class ServiceabilityCheckRequest {
    @Pattern(regexp = "^(\\d{6})?$", message = "invalid pincode")
    public String pincode;
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonAlias({"lat"})
    public Double latitude;
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonAlias({"lng", "lon"})
    public Double longitude;
}
