package com.lexho.user_service.office.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ScreeningCentreRequest(
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,
        @NotBlank(message = "address is required")
        @Size(max = 500, message = "address must be at most 500 characters")
        String address,
        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
        BigDecimal latitude,
        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
        BigDecimal longitude,
        @Size(max = 20, message = "phone must be at most 20 characters")
        @Pattern(regexp = "^(\\+91[6-9][0-9]{9}|[6-9][0-9]{9})$|^$", message = "phone must be a valid Indian mobile number")
        String phone,
        @Size(max = 50, message = "mapProvider must be at most 50 characters")
        String mapProvider,
        @Size(max = 150, message = "placeId must be at most 150 characters")
        String placeId,
        @Size(max = 255, message = "landmark must be at most 255 characters")
        String landmark,
        @Size(max = 255, message = "entranceInfo must be at most 255 characters")
        String entranceInfo) {
}
