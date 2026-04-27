package com.lexho.user_service.admin.dto;

public class AdminServiceRatingAnalyticsResponse {

    private String category;
    private double avgRating;
    private double maxRating;
    private double minRating;
    private long servicesCount;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public double getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(double maxRating) {
        this.maxRating = maxRating;
    }

    public double getMinRating() {
        return minRating;
    }

    public void setMinRating(double minRating) {
        this.minRating = minRating;
    }

    public long getServicesCount() {
        return servicesCount;
    }

    public void setServicesCount(long servicesCount) {
        this.servicesCount = servicesCount;
    }
}

