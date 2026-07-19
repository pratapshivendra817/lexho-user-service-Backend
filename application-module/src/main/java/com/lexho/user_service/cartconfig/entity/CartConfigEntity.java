package com.lexho.user_service.cartconfig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_config")
public class CartConfigEntity {
    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Integer maxCartItems = 10;
    @Column(nullable = false)
    private Integer minServiceQuantity = 1;
    @Column(nullable = false)
    private Integer maxServiceQuantity = 5;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal minCleaningDurationHours = BigDecimal.valueOf(0.5d);
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal maxCleaningDurationHours = BigDecimal.valueOf(8);
    @Column(nullable = false)
    private Integer instantBookingWindowMinutes = 30;
    @Column(nullable = false)
    private Integer scheduledBookingWindowDays = 4;
    @Column(nullable = false)
    private Integer slotIntervalMinutes = 15;
    @Column(nullable = false)
    private Boolean instantBookingEnabled = true;
    @Column(nullable = false)
    private Boolean scheduledBookingEnabled = true;
    @Column(nullable = false)
    private Boolean subscriptionBookingEnabled = true;
    @Column(nullable = false)
    private Boolean requireSlotRecheck = true;
    @Column(nullable = false)
    private Boolean rejectFullSlots = true;
    @Column(nullable = false)
    private Integer maxBookingsPerSlot = 1;
    @Column(nullable = false)
    private Boolean subscriptionCheckoutEnabled = true;
    @Column(nullable = false)
    private Boolean subscriptionPaymentRequired = true;
    @Column(nullable = false)
    private Boolean subscriptionAutoRenewEnabled = false;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMaxCartItems() { return maxCartItems; }
    public void setMaxCartItems(Integer maxCartItems) { this.maxCartItems = maxCartItems; }
    public Integer getMinServiceQuantity() { return minServiceQuantity; }
    public void setMinServiceQuantity(Integer minServiceQuantity) { this.minServiceQuantity = minServiceQuantity; }
    public Integer getMaxServiceQuantity() { return maxServiceQuantity; }
    public void setMaxServiceQuantity(Integer maxServiceQuantity) { this.maxServiceQuantity = maxServiceQuantity; }
    public BigDecimal getMinCleaningDurationHours() { return minCleaningDurationHours; }
    public void setMinCleaningDurationHours(BigDecimal minCleaningDurationHours) { this.minCleaningDurationHours = minCleaningDurationHours; }
    public BigDecimal getMaxCleaningDurationHours() { return maxCleaningDurationHours; }
    public void setMaxCleaningDurationHours(BigDecimal maxCleaningDurationHours) { this.maxCleaningDurationHours = maxCleaningDurationHours; }
    public Integer getInstantBookingWindowMinutes() { return instantBookingWindowMinutes; }
    public void setInstantBookingWindowMinutes(Integer instantBookingWindowMinutes) { this.instantBookingWindowMinutes = instantBookingWindowMinutes; }
    public Integer getScheduledBookingWindowDays() { return scheduledBookingWindowDays; }
    public void setScheduledBookingWindowDays(Integer scheduledBookingWindowDays) { this.scheduledBookingWindowDays = scheduledBookingWindowDays; }
    public Integer getSlotIntervalMinutes() { return slotIntervalMinutes; }
    public void setSlotIntervalMinutes(Integer slotIntervalMinutes) { this.slotIntervalMinutes = slotIntervalMinutes; }
    public Boolean getInstantBookingEnabled() { return instantBookingEnabled; }
    public void setInstantBookingEnabled(Boolean instantBookingEnabled) { this.instantBookingEnabled = instantBookingEnabled; }
    public Boolean getScheduledBookingEnabled() { return scheduledBookingEnabled; }
    public void setScheduledBookingEnabled(Boolean scheduledBookingEnabled) { this.scheduledBookingEnabled = scheduledBookingEnabled; }
    public Boolean getSubscriptionBookingEnabled() { return subscriptionBookingEnabled; }
    public void setSubscriptionBookingEnabled(Boolean subscriptionBookingEnabled) { this.subscriptionBookingEnabled = subscriptionBookingEnabled; }
    public Boolean getRequireSlotRecheck() { return requireSlotRecheck; }
    public void setRequireSlotRecheck(Boolean requireSlotRecheck) { this.requireSlotRecheck = requireSlotRecheck; }
    public Boolean getRejectFullSlots() { return rejectFullSlots; }
    public void setRejectFullSlots(Boolean rejectFullSlots) { this.rejectFullSlots = rejectFullSlots; }
    public Integer getMaxBookingsPerSlot() { return maxBookingsPerSlot; }
    public void setMaxBookingsPerSlot(Integer maxBookingsPerSlot) { this.maxBookingsPerSlot = maxBookingsPerSlot; }
    public Boolean getSubscriptionCheckoutEnabled() { return subscriptionCheckoutEnabled; }
    public void setSubscriptionCheckoutEnabled(Boolean subscriptionCheckoutEnabled) { this.subscriptionCheckoutEnabled = subscriptionCheckoutEnabled; }
    public Boolean getSubscriptionPaymentRequired() { return subscriptionPaymentRequired; }
    public void setSubscriptionPaymentRequired(Boolean subscriptionPaymentRequired) { this.subscriptionPaymentRequired = subscriptionPaymentRequired; }
    public Boolean getSubscriptionAutoRenewEnabled() { return subscriptionAutoRenewEnabled; }
    public void setSubscriptionAutoRenewEnabled(Boolean subscriptionAutoRenewEnabled) { this.subscriptionAutoRenewEnabled = subscriptionAutoRenewEnabled; }
}
