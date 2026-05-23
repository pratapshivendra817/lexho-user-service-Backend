package com.lexho.user_service.admin.serviceablearea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ServiceableAreaUpsertRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    private String name;

    @NotBlank(message = "city is required")
    @Size(max = 120, message = "city must be at most 120 characters")
    private String city;

    @NotBlank(message = "state is required")
    @Size(max = 120, message = "state must be at most 120 characters")
    private String state;

    @NotBlank(message = "pincode is required")
    @Pattern(regexp = "^[0-9]{4,10}$", message = "pincode must be 4 to 10 digits")
    private String pincode;

    private Long cityId;
    private Long zoneId;
    private String polygonJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getPolygonJson() {
        return polygonJson;
    }

    public void setPolygonJson(String polygonJson) {
        this.polygonJson = polygonJson;
    }
}
