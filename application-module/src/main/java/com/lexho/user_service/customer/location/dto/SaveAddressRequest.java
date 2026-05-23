package com.lexho.user_service.customer.location.dto;

import com.lexho.user_service.customer.location.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SaveAddressRequest {
    @NotNull
    public AddressType type;
    public String customLabel;
    public String flatNo;
    public String floor;
    public String buildingName;
    public String landmark;
    @NotBlank
    public String addressLine;
    public String area;
    public String sector;
    @NotNull
    public Double latitude;
    @NotNull
    public Double longitude;
    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "invalid mobile")
    public String mobile;
}
