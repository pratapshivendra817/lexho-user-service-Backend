package com.lexho.user_service.partnerrequest.dto;

import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;

import java.time.LocalDateTime;

public class PartnerIncomingRequestResponse {
    private Long requestId;
    private Long cartId;
    private Long customerId;
    private Long serviceId;
    private String serviceType;
    private Integer quantity;
    private LocalDateTime scheduledTime;
    private PartnerServiceRequestStatus status;
    private LocalDateTime createdAt;
    private Integer etaMinutes;
    private LocalDateTime expiresAt;
    private PartnerRequestAddressResponse address;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public PartnerServiceRequestStatus getStatus() { return status; }
    public void setStatus(PartnerServiceRequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public PartnerRequestAddressResponse getAddress() { return address; }
    public void setAddress(PartnerRequestAddressResponse address) { this.address = address; }
}
