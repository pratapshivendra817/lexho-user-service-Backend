package com.lexho.user_service.partnerrequest.dto;

import jakarta.validation.constraints.NotNull;

public class PartnerStatusUpdateRequest {
    @NotNull(message = "isOnline is required")
    private Boolean isOnline;

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }
}
