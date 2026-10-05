package com.lexho.user_service.admin.geo.service;

final class AdminGeoText {

    private AdminGeoText() {
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
