package com.lexho.auth.dto;

import java.util.Objects;

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

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TokenPairResponse that)) {
            return false;
        }
        return Objects.equals(accessToken, that.accessToken) && Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken);
    }

    @Override
    public String toString() {
        return "TokenPairResponse{"
                + "accessToken='" + accessToken + '\''
                + ", refreshToken='" + refreshToken + '\''
                + '}';
    }
}
