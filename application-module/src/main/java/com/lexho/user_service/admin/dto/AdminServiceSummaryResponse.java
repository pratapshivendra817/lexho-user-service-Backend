package com.lexho.user_service.admin.dto;

public class AdminServiceSummaryResponse {

    private String category;
    private long totalServices;
    private long totalBookings;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(long totalServices) {
        this.totalServices = totalServices;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }
}

