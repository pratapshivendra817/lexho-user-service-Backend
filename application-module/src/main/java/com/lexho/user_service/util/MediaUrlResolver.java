package com.lexho.user_service.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class MediaUrlResolver {

    public String toPublicUrl(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (isAbsoluteUrl(normalized)) {
            return normalized;
        }
        String path = normalized.startsWith("/") ? normalized : "/" + normalized;
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(path)
                    .toUriString();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return path;
        }
    }

    public String toStorablePath(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (!isAbsoluteUrl(normalized)) {
            return normalized.startsWith("/") ? normalized : "/" + normalized;
        }
        try {
            URI uri = URI.create(normalized);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return normalized;
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (IllegalArgumentException ex) {
            return normalized;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isAbsoluteUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
