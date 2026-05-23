package com.lexho.user_service.partnerrequest.dto;

public class PartnerStatusResponse {
    private Long partnerId;
    private boolean online;

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
