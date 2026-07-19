package com.lexho.user_service.addon.dto;

public class CartAddonResponse {
    private Long addonId;
    private String addonName;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
    private boolean required;
    private boolean quantityAllowed;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public String getAddonName() { return addonName; }
    public void setAddonName(String addonName) { this.addonName = addonName; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isQuantityAllowed() { return quantityAllowed; }
    public void setQuantityAllowed(boolean quantityAllowed) { this.quantityAllowed = quantityAllowed; }
}
