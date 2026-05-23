package com.lexho.user_service.banner.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum BannerPlatformType {
    ANDROID,
    IOS,
    WEB,
    ALL;

    @JsonCreator
    public static BannerPlatformType fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("MOBILE".equals(normalized)) {
            return ALL;
        }
        try {
            return BannerPlatformType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return ALL;
        }
    }
}
