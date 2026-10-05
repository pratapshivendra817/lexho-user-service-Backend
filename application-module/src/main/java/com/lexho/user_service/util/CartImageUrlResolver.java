package com.lexho.user_service.util;

import java.net.URI;
import java.util.Locale;

public final class CartImageUrlResolver {

    private static final String DEFAULT_BASE_URL = "https://cdn.lexho.com";
    private static final String DEFAULT_SERVICE_IMAGE_PATH = "/defaults/service.png";

    private CartImageUrlResolver() {
    }

    public static String toDeviceReachableImageUrl(String raw, String mediaPublicBaseUrl) {
        String base = sanitizeBaseUrl(mediaPublicBaseUrl);
        if (raw == null || raw.isBlank()) {
            return base + DEFAULT_SERVICE_IMAGE_PATH;
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (isLoopbackUrl(lower)) {
                String path = extractPath(trimmed);
                return path == null ? base + DEFAULT_SERVICE_IMAGE_PATH : base + path;
            }
            return lower.startsWith("http://") ? "https://" + trimmed.substring(7) : trimmed;
        }
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + normalized;
    }

    private static boolean isLoopbackUrl(String lowerUrl) {
        return lowerUrl.contains("localhost")
                || lowerUrl.contains("127.0.0.1")
                || lowerUrl.contains("0.0.0.0");
    }

    private static String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String extractPath(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
