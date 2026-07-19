package com.lexho.user_service.addon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AddonServiceMappingRequest {
    @NotNull(message = "addonId is required")
    private Long addonId;

    @NotEmpty(message = "serviceIds is required")
    private List<Long> serviceIds;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public List<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<Long> serviceIds) { this.serviceIds = serviceIds; }
}
