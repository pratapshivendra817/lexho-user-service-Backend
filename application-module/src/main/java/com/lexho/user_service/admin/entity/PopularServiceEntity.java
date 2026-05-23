package com.lexho.user_service.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_services", indexes = {
        @Index(name = "idx_popular_services_category_active_priority", columnList = "category_id,is_active,priority")
})
public class PopularServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private AdminServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AdminCategoryEntity category;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.active = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AdminServiceEntity getService() {
        return service;
    }

    public void setService(AdminServiceEntity service) {
        this.service = service;
    }

    public AdminCategoryEntity getCategory() {
        return category;
    }

    public void setCategory(AdminCategoryEntity category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
