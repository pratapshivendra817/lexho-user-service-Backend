package com.lexho.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.exposure")
public class SecurityExposureProperties {

    private boolean exposeSwagger = true;
    private boolean allowInternalOpen = true;
    private boolean allowPublicCustomerRead = true;
    private boolean allowPublicProviderOnboarding = true;
    private boolean allowPublicAdminProfileContent = true;

    public boolean isExposeSwagger() {
        return exposeSwagger;
    }

    public void setExposeSwagger(boolean exposeSwagger) {
        this.exposeSwagger = exposeSwagger;
    }

    public boolean isAllowInternalOpen() {
        return allowInternalOpen;
    }

    public void setAllowInternalOpen(boolean allowInternalOpen) {
        this.allowInternalOpen = allowInternalOpen;
    }

    public boolean isAllowPublicCustomerRead() {
        return allowPublicCustomerRead;
    }

    public void setAllowPublicCustomerRead(boolean allowPublicCustomerRead) {
        this.allowPublicCustomerRead = allowPublicCustomerRead;
    }

    public boolean isAllowPublicProviderOnboarding() {
        return allowPublicProviderOnboarding;
    }

    public void setAllowPublicProviderOnboarding(boolean allowPublicProviderOnboarding) {
        this.allowPublicProviderOnboarding = allowPublicProviderOnboarding;
    }

    public boolean isAllowPublicAdminProfileContent() {
        return allowPublicAdminProfileContent;
    }

    public void setAllowPublicAdminProfileContent(boolean allowPublicAdminProfileContent) {
        this.allowPublicAdminProfileContent = allowPublicAdminProfileContent;
    }
}
