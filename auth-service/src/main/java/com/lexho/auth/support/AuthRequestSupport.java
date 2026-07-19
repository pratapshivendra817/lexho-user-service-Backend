package com.lexho.auth.support;

import com.lexho.common.enums.Role;

import java.util.Locale;

public final class AuthRequestSupport {

    private AuthRequestSupport() {
    }

    public static Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid role");
        }
    }

    public static String normalizeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }
        return refreshToken.trim();
    }
}
