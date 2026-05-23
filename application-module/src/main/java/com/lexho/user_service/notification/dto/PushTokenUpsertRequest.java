package com.lexho.user_service.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public class PushTokenUpsertRequest {
    @NotBlank(message = "deviceId is required")
    @Size(max = 120, message = "deviceId too long")
    public String deviceId;
    @Size(max = 500, message = "token too long")
    public String token;

    @Size(max = 500, message = "fcmToken too long")
    public String fcmToken;

    @Size(max = 30, message = "platform too long")
    public String platform;

    @Size(max = 30, message = "appType too long")
    public String appType;

    @AssertTrue(message = "token or fcmToken is required")
    public boolean isTokenPresent() {
        return hasText(token) || hasText(fcmToken);
    }

    public String resolvedToken() {
        String value = hasText(fcmToken) ? fcmToken : token;
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
