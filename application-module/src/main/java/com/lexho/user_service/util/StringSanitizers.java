package com.lexho.user_service.util;

import com.lexho.user_service.exception.BadRequestException;

public final class StringSanitizers {

    private StringSanitizers() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String requireTrimmed(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }
}
