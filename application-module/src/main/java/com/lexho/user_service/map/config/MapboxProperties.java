package com.lexho.user_service.map.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mapbox")
public class MapboxProperties {
    private boolean enabled = false;
    private String accessToken;
    private String geocodingUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places";
    private String directionsUrl = "https://api.mapbox.com/directions/v5/mapbox";
    private String navigationProfile = "driving-traffic";
    private long routeCacheTtlSeconds = 30L;
    private double routeDeviationThresholdMeters = 250.0d;
    private double routeRecalculationMovementMeters = 75.0d;
    private int etaWorseningThresholdMinutes = 3;
    private long liveEtaTrackingTtlSeconds = 300L;

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

    public String getNavigationProfile() {
        return navigationProfile;
    }

    public void setNavigationProfile(String navigationProfile) {
        this.navigationProfile = navigationProfile;
    }

    public long getRouteCacheTtlSeconds() {
        return routeCacheTtlSeconds;
    }

    public void setRouteCacheTtlSeconds(long routeCacheTtlSeconds) {
        this.routeCacheTtlSeconds = routeCacheTtlSeconds;
    }

    public double getRouteDeviationThresholdMeters() {
        return routeDeviationThresholdMeters;
    }

    public void setRouteDeviationThresholdMeters(double routeDeviationThresholdMeters) {
        this.routeDeviationThresholdMeters = routeDeviationThresholdMeters;
    }

    public double getRouteRecalculationMovementMeters() {
        return routeRecalculationMovementMeters;
    }

    public void setRouteRecalculationMovementMeters(double routeRecalculationMovementMeters) {
        this.routeRecalculationMovementMeters = routeRecalculationMovementMeters;
    }

    public int getEtaWorseningThresholdMinutes() {
        return etaWorseningThresholdMinutes;
    }

    public void setEtaWorseningThresholdMinutes(int etaWorseningThresholdMinutes) {
        this.etaWorseningThresholdMinutes = etaWorseningThresholdMinutes;
    }

    public long getLiveEtaTrackingTtlSeconds() {
        return liveEtaTrackingTtlSeconds;
    }

    public void setLiveEtaTrackingTtlSeconds(long liveEtaTrackingTtlSeconds) {
        this.liveEtaTrackingTtlSeconds = liveEtaTrackingTtlSeconds;
    }
}
