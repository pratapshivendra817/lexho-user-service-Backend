package com.lexho.user_service.addon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AddonResponse {
    private Long addonId;
    private String addonName;
    private String description;
    private BigDecimal price;
    private boolean active;
    private boolean quantityAllowed;
    private Integer minQuantity;
    private Integer maxQuantity;
    private boolean required;
    private List<Long> categoryIds;
    private List<Long> serviceIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public String getAddonName() { return addonName; }
    public void setAddonName(String addonName) { this.addonName = addonName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isQuantityAllowed() { return quantityAllowed; }
    public void setQuantityAllowed(boolean quantityAllowed) { this.quantityAllowed = quantityAllowed; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public List<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<Long> serviceIds) { this.serviceIds = serviceIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
