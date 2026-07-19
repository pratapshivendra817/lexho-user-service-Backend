package com.lexho.user_service.subscription.entity;

import com.lexho.user_service.subscription.enums.FamilySize;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cook_subscription_pricing", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cook_subscription_pricing_service_family", columnNames = {"service_id", "family_size"})
})
public class CookSubscriptionPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_size", nullable = false, length = 20)
    private FamilySize familySize;

    @Column(name = "price_per_visit", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerVisit;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public FamilySize getFamilySize() { return familySize; }
    public void setFamilySize(FamilySize familySize) { this.familySize = familySize; }
    public BigDecimal getPricePerVisit() { return pricePerVisit; }
    public void setPricePerVisit(BigDecimal pricePerVisit) { this.pricePerVisit = pricePerVisit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
