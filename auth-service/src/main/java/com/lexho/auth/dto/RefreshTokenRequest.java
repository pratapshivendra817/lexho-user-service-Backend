package com.lexho.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;

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
        if (!(object instanceof RefreshTokenRequest that)) {
            return false;
        }
        return Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refreshToken);
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{"
                + "refreshToken='" + refreshToken + '\''
                + '}';
    }
}
