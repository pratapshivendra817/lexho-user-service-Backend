package com.lexho.user_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlaceOrderRequest {
    private String address;
    private LocalDateTime scheduledAt;
    private String bookingType;
    private BigDecimal expectedTotal;
    private String couponCode;
    private Long dispatchAddressId;
    private Long dispatchServiceId;
    private Integer dispatchQuantity;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public BigDecimal getExpectedTotal() {
        return expectedTotal;
    }

    public void setExpectedTotal(BigDecimal expectedTotal) {
        this.expectedTotal = expectedTotal;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Long getDispatchAddressId() {
        return dispatchAddressId;
    }

    public void setDispatchAddressId(Long dispatchAddressId) {
        this.dispatchAddressId = dispatchAddressId;
    }

    public Long getDispatchServiceId() {
        return dispatchServiceId;
    }

    public void setDispatchServiceId(Long dispatchServiceId) {
        this.dispatchServiceId = dispatchServiceId;
    }

    public Integer getDispatchQuantity() {
        return dispatchQuantity;
    }

    public void setDispatchQuantity(Integer dispatchQuantity) {
        this.dispatchQuantity = dispatchQuantity;
    }
}
