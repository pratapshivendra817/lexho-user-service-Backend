package com.lexho.auth.dto;

public class TokenPairResponse {

    private String accessToken;
    private String refreshToken;

    public TokenPairResponse() {
    }

    public TokenPairResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
