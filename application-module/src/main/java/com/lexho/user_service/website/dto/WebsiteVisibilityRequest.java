package com.lexho.user_service.website.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class WebsiteVisibilityRequest {
    @NotNull
    private Boolean showOnWebsite;

    @Min(0)
    private Integer displayOrder;

    public Boolean getShowOnWebsite() { return showOnWebsite; }
    public void setShowOnWebsite(Boolean showOnWebsite) { this.showOnWebsite = showOnWebsite; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
