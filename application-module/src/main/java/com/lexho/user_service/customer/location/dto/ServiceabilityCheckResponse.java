package com.lexho.user_service.customer.location.dto;

import java.util.ArrayList;
import java.util.List;

public class ServiceabilityCheckResponse {
    private boolean serviceable;
    private String reason;
    private Long matchedCityId;
    private Long matchedZoneId;
    private String matchedPincode;
    private List<String> serviceZones = new ArrayList<>();
    private List<String> supportedServices = new ArrayList<>();

    public boolean isServiceable() {
        return serviceable;
    }

    public void setServiceable(boolean serviceable) {
        this.serviceable = serviceable;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getMatchedCityId() {
        return matchedCityId;
    }

    public void setMatchedCityId(Long matchedCityId) {
        this.matchedCityId = matchedCityId;
    }

    public Long getMatchedZoneId() {
        return matchedZoneId;
    }

    public void setMatchedZoneId(Long matchedZoneId) {
        this.matchedZoneId = matchedZoneId;
    }

    public String getMatchedPincode() {
        return matchedPincode;
    }

    public void setMatchedPincode(String matchedPincode) {
        this.matchedPincode = matchedPincode;
    }

    public List<String> getServiceZones() {
        return serviceZones;
    }

    public void setServiceZones(List<String> serviceZones) {
        this.serviceZones = serviceZones;
    }

    public List<String> getSupportedServices() {
        return supportedServices;
    }

    public void setSupportedServices(List<String> supportedServices) {
        this.supportedServices = supportedServices;
    }
}
