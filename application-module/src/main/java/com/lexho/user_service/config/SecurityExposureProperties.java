package com.lexho.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.exposure")
public class SecurityExposureProperties {

    private boolean exposeSwagger = false;
    private boolean allowInternalOpen = false;
    private boolean allowPublicCustomerRead = false;
    private boolean allowPublicAdminProfileContent = false;
    private boolean allowPartnerRequestDevSeed = false;

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

    public boolean isAllowPublicAdminProfileContent() {
        return allowPublicAdminProfileContent;
    }

    public void setAllowPublicAdminProfileContent(boolean allowPublicAdminProfileContent) {
        this.allowPublicAdminProfileContent = allowPublicAdminProfileContent;
    }

    public boolean isAllowPartnerRequestDevSeed() {
        return allowPartnerRequestDevSeed;
    }

    public void setAllowPartnerRequestDevSeed(boolean allowPartnerRequestDevSeed) {
        this.allowPartnerRequestDevSeed = allowPartnerRequestDevSeed;
    }
}
