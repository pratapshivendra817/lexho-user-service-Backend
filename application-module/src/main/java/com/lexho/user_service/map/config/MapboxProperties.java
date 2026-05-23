package com.lexho.user_service.map.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mapbox")
public class MapboxProperties {
    private boolean enabled = false;
    private String accessToken;
    private String geocodingUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places";
    private String directionsUrl = "https://api.mapbox.com/directions/v5/mapbox";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getGeocodingUrl() {
        return geocodingUrl;
    }

    public void setGeocodingUrl(String geocodingUrl) {
        this.geocodingUrl = geocodingUrl;
    }

    public String getDirectionsUrl() {
        return directionsUrl;
    }

    public void setDirectionsUrl(String directionsUrl) {
        this.directionsUrl = directionsUrl;
    }
}
