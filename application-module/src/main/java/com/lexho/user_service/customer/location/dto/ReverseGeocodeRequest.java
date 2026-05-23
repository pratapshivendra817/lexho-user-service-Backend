package com.lexho.user_service.customer.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class ReverseGeocodeRequest {
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    public Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    public Double longitude;
}
