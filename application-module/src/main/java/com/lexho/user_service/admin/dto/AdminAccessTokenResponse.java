package com.lexho.user_service.admin.dto;

public class AdminAccessTokenResponse {
    private String accessToken;

    public AdminAccessTokenResponse() {
    }

    public AdminAccessTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
