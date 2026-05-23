package com.lexho.user_service.partnerrequest.dto;

import jakarta.validation.constraints.Size;

public class PartnerRejectRequest {
    @Size(max = 255, message = "Reason must be <= 255 characters")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
