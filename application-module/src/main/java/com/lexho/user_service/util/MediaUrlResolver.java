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
        String uploadPath = extractUploadPath(normalized);
        if (uploadPath != null) {
            normalized = uploadPath;
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
            String uploadPath = extractUploadPath(normalized);
            if (uploadPath != null) {
                return uploadPath;
            }
            return normalized.startsWith("/") ? normalized : "/" + normalized;
        }
        try {
            URI uri = URI.create(normalized);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return normalized;
            }
            String uploadPath = extractUploadPath(path);
            if (uploadPath != null) {
                return uploadPath;
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (IllegalArgumentException ex) {
            return normalized;
        }
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }

    private boolean isAbsoluteUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String extractUploadPath(String value) {
        String normalizedSeparators = value.replace('\\', '/');
        String lower = normalizedSeparators.toLowerCase();
        int uploadsIndex = lower.lastIndexOf("/uploads/");
        if (uploadsIndex >= 0) {
            return normalizedSeparators.substring(uploadsIndex);
        }
        if (lower.startsWith("uploads/")) {
            return "/" + normalizedSeparators;
        }
        if (lower.startsWith("/images/") || lower.startsWith("/uploads/")) {
            return normalizedSeparators;
        }
        if (lower.startsWith("images/")) {
            return "/" + normalizedSeparators;
        }
        return null;
    }
}
