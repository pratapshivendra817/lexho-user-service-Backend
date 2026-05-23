package com.lexho.user_service.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminProfileContentRequest {

    @NotBlank(message = "aboutUs is required")
    private String aboutUs;

    @NotBlank(message = "termsOfService is required")
    private String termsOfService;

    @NotBlank(message = "privacyPolicy is required")
    private String privacyPolicy;

    public String getAboutUs() {
        return aboutUs;
    }

    public void setAboutUs(String aboutUs) {
        this.aboutUs = aboutUs;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public String getPrivacyPolicy() {
        return privacyPolicy;
    }

    public void setPrivacyPolicy(String privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }
}
