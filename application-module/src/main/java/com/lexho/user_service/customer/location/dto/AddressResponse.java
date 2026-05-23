package com.lexho.user_service.customer.location.dto;

import com.lexho.user_service.customer.location.entity.AddressType;

import java.util.UUID;

public class AddressResponse {
    public UUID id;
    public AddressType type;
    public String customLabel;
    public String flatNo;
    public String floor;
    public String buildingName;
    public String landmark;
    public String addressLine;
    public String area;
    public String sector;
    public String city;
    public String state;
    public String country;
    public String pincode;
    public Double latitude;
    public Double longitude;
    public String mobile;
    public boolean isDefault;
}
