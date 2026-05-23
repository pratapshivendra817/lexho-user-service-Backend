package com.lexho.user_service.admin.dto;

import java.time.LocalDateTime;

public class AdminCategoryResponse {

    private Long id;
    private String name;
    private String iconUrl;
    private String imageUrl;
    private boolean active;
    private long totalServicesCount;
    private long totalBookingsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTotalServicesCount() {
        return totalServicesCount;
    }

    public void setTotalServicesCount(long totalServicesCount) {
        this.totalServicesCount = totalServicesCount;
    }

    public long getTotalBookingsCount() {
        return totalBookingsCount;
    }

    public void setTotalBookingsCount(long totalBookingsCount) {
        this.totalBookingsCount = totalBookingsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
