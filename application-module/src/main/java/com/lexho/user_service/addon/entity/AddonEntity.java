package com.lexho.user_service.addon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "addons")
public class AddonEntity {
    private static final ZoneId APPLICATION_ZONE = ZoneId.systemDefault();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "addon_name", nullable = false, length = 150)
    private String addonName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "quantity_allowed", nullable = false)
    private boolean quantityAllowed;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    @Column(name = "max_quantity", nullable = false)
    private Integer maxQuantity = 1;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = now();
        createdAt = now;
        updatedAt = now;
        normalizeQuantities();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = now();
        normalizeQuantities();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(APPLICATION_ZONE);
    }

    public void normalizeQuantities() {
        if (minQuantity == null || minQuantity < 1) {
            minQuantity = 1;
        }
        if (!quantityAllowed) {
            maxQuantity = 1;
            minQuantity = Math.min(minQuantity, 1);
        }
        if (maxQuantity == null || maxQuantity < minQuantity) {
            maxQuantity = minQuantity;
        }
    }

    public Long getId() { return id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
