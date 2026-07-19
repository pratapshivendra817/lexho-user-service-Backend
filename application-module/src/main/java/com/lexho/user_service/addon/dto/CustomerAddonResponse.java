package com.lexho.user_service.addon.dto;

import java.math.BigDecimal;

public class CustomerAddonResponse {
    private Long addonId;
    private String addonName;
    private String description;
    private BigDecimal price;
    private boolean required;
    private boolean quantityAllowed;
    private Integer minQuantity;
    private Integer maxQuantity;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public String getAddonName() { return addonName; }
    public void setAddonName(String addonName) { this.addonName = addonName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isQuantityAllowed() { return quantityAllowed; }
    public void setQuantityAllowed(boolean quantityAllowed) { this.quantityAllowed = quantityAllowed; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
}
