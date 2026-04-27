package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminCategoryUpsertRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 120, message = "Category name must be at most 120 characters")
    private String name;

    @Size(max = 500, message = "Icon URL must be at most 500 characters")
    private String iconUrl;

    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
