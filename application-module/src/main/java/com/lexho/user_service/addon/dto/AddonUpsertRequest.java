package com.lexho.user_service.addon.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class AddonUpsertRequest {
    @JsonAlias("name")
    @NotBlank(message = "addonName is required")
    @Size(max = 150, message = "addonName too long")
    private String addonName;

    @Size(max = 1000, message = "description too long")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", message = "price must be non-negative")
    private BigDecimal price;

    private Boolean active;
    private Boolean quantityAllowed;

    @Min(value = 1, message = "minQuantity must be at least 1")
    private Integer minQuantity;

    @Min(value = 1, message = "maxQuantity must be at least 1")
    private Integer maxQuantity;

    private Boolean required;
    private List<Long> categoryIds;
    private List<Long> serviceIds;

    public String getAddonName() { return addonName; }
    public void setAddonName(String addonName) { this.addonName = addonName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getQuantityAllowed() { return quantityAllowed; }
    public void setQuantityAllowed(Boolean quantityAllowed) { this.quantityAllowed = quantityAllowed; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public List<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<Long> serviceIds) { this.serviceIds = serviceIds; }
}
