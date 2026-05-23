package com.lexho.user_service.customer.location.dto;

import com.lexho.user_service.customer.location.entity.AddressType;
import jakarta.validation.constraints.Pattern;

public class UpdateAddressRequest {
    public AddressType type;
    public String customLabel;
    public String flatNo;
    public String floor;
    public String buildingName;
    public String landmark;
    public String addressLine;
    public String area;
    public String sector;
    public Double latitude;
    public Double longitude;
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "invalid mobile")
    public String mobile;
}
