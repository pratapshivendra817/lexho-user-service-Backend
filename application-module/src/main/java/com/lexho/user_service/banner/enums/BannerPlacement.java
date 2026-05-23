package com.lexho.user_service.banner.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum BannerPlacement {
    TOP,
    BOTTOM;

    @JsonCreator
    public static BannerPlacement fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return TOP;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("HOME_TOP".equals(normalized)) {
            return TOP;
        }
        if ("HOME_BOTTOM".equals(normalized)) {
            return BOTTOM;
        }
        return BannerPlacement.valueOf(normalized);
    }
}
