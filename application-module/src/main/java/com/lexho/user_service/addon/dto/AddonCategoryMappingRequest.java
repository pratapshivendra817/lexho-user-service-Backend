package com.lexho.user_service.addon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AddonCategoryMappingRequest {
    @NotNull(message = "addonId is required")
    private Long addonId;

    @NotEmpty(message = "categoryIds is required")
    private List<Long> categoryIds;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
}
