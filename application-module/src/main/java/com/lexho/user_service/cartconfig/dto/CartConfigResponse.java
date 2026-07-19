package com.lexho.user_service.cartconfig.dto;

import java.math.BigDecimal;

public class CartConfigResponse {
    private Integer maxCartItems;
    private Integer minServiceQuantity;
    private Integer maxServiceQuantity;
    private BigDecimal minCleaningDurationHours;
    private BigDecimal maxCleaningDurationHours;
    private Integer instantBookingWindowMinutes;
    private Integer scheduledBookingWindowDays;
    private Integer slotIntervalMinutes;
    private Boolean instantBookingEnabled;
    private Boolean scheduledBookingEnabled;
    private Boolean subscriptionBookingEnabled;
    private Boolean requireSlotRecheck;
    private Boolean rejectFullSlots;
    private Integer maxBookingsPerSlot;
    private Boolean subscriptionCheckoutEnabled;
    private Boolean subscriptionPaymentRequired;
    private Boolean subscriptionAutoRenewEnabled;

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
