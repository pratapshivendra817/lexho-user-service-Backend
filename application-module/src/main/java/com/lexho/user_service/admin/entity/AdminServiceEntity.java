package com.lexho.user_service.admin.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
public class AdminServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private AdminCategoryEntity category;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "total_bookings")
    private Long totalBookings;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "category_name", length = 120)
    private String categoryName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null || this.status.isBlank()) {
            this.status = this.active ? "ACTIVE" : "INACTIVE";
        }
        if (this.totalBookings == null) {
            this.totalBookings = 0L;
        }
        if (this.avgRating == null) {
            this.avgRating = 0.0;
        }
        syncCategoryName();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.status = this.active ? "ACTIVE" : "INACTIVE";
        syncCategoryName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdminCategoryEntity getCategory() {
        return category;
    }

    public void setCategory(AdminCategoryEntity category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? "ACTIVE" : "INACTIVE";
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.active = status != null && "ACTIVE".equalsIgnoreCase(status);
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void syncCategoryName() {
        if (this.category != null && this.category.getName() != null && !this.category.getName().isBlank()) {
            this.categoryName = this.category.getName();
        }
    }
}
