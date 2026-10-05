package com.lexho.user_service.partnerrequest.dto;

import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;

public class PartnerRequestActionResponse {
    private Long requestId;
    private Long orderId;
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    private PartnerServiceRequestStatus status;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public PartnerServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PartnerServiceRequestStatus status) {
        this.status = status;
    }
}
