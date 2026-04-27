package com.lexho.user_service.dto;

public class HomeBannerDTO {
    private String label;
    private String ctaLabel;
    private String secondaryCtaLabel;
    private String tagline;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCtaLabel() {
        return ctaLabel;
    }

    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    public String getSecondaryCtaLabel() {
        return secondaryCtaLabel;
    }

    public void setSecondaryCtaLabel(String secondaryCtaLabel) {
        this.secondaryCtaLabel = secondaryCtaLabel;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }
}

