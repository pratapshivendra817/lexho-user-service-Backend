package com.lexho.user_service.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push.fcm")
public class FcmProperties {

    private boolean enabled = false;
    private String serviceAccountPath;
    private String serviceAccountBase64;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    public String getServiceAccountBase64() {
        return serviceAccountBase64;
    }

    public void setServiceAccountBase64(String serviceAccountBase64) {
        this.serviceAccountBase64 = serviceAccountBase64;
    }
}

