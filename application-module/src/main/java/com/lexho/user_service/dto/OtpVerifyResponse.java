package com.lexho.user_service.dto;

public class OtpVerifyResponse {

    private String token;
    private Long userId;
    private String mobile;
    private boolean active;

    public OtpVerifyResponse() {
    }

    public OtpVerifyResponse(String token, Long userId, String mobile, boolean active) {
        this.token = token;
        this.userId = userId;
        this.mobile = mobile;
        this.active = active;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

