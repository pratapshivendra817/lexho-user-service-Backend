package com.lexho.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final UserDefaults user = new UserDefaults();
    private final HomeDefaults home = new HomeDefaults();

    public UserDefaults getUser() {
        return user;
    }

    public HomeDefaults getHome() {
        return home;
    }

    public static class UserDefaults {
        private String defaultCity = "Unknown";

        public String getDefaultCity() {
            return defaultCity;
        }

        public void setDefaultCity(String defaultCity) {
            this.defaultCity = defaultCity;
        }
    }

    public static class HomeDefaults {
        private String primaryPromoLabel = "Limited Offer";
        private String primaryCtaLabel = "Book Now";
        private String secondaryCtaLabel = "View Offers";
        private String recommendationTagline = "Next available slot soon";
        private String fallbackGreeting = "Hi";

        public String getPrimaryPromoLabel() {
            return primaryPromoLabel;
        }

        public void setPrimaryPromoLabel(String primaryPromoLabel) {
            this.primaryPromoLabel = primaryPromoLabel;
        }

        public String getPrimaryCtaLabel() {
            return primaryCtaLabel;
        }

        public void setPrimaryCtaLabel(String primaryCtaLabel) {
            this.primaryCtaLabel = primaryCtaLabel;
        }

        public String getSecondaryCtaLabel() {
            return secondaryCtaLabel;
        }

        public void setSecondaryCtaLabel(String secondaryCtaLabel) {
            this.secondaryCtaLabel = secondaryCtaLabel;
        }

        public String getRecommendationTagline() {
            return recommendationTagline;
        }

        public void setRecommendationTagline(String recommendationTagline) {
            this.recommendationTagline = recommendationTagline;
        }

        public String getFallbackGreeting() {
            return fallbackGreeting;
        }

        public void setFallbackGreeting(String fallbackGreeting) {
            this.fallbackGreeting = fallbackGreeting;
        }
    }
}

