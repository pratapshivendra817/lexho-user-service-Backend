package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdminCategoryUpsertRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 120, message = "Category name must be at most 120 characters")
    private String name;

    @Size(max = 1000, message = "Category description must be at most 1000 characters")
    private String description;

    @Size(max = 500, message = "Icon URL must be at most 500 characters")
    private String iconUrl;

    private Boolean active;

    @NotNull(message = "displayOrder is required")
    @Min(value = 1, message = "displayOrder must be greater than or equal to 1")
    private Integer displayOrder;

    @DecimalMin(value = "0.0", inclusive = true, message = "Commission percent must be at least 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Commission percent must be at most 100")
    private BigDecimal commissionPercent;

    private Boolean reviewEnabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public Boolean getReviewEnabled() {
        return reviewEnabled;
    }

    public void setReviewEnabled(Boolean reviewEnabled) {
        this.reviewEnabled = reviewEnabled;
    }
}
