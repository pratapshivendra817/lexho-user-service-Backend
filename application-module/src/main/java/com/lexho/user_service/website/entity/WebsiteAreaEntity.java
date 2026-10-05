package com.lexho.user_service.website.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "website_areas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_website_areas_city_slug", columnNames = {"city_id", "slug"})
}, indexes = {
        @Index(name = "idx_website_areas_city_active", columnList = "city_id,is_active,is_deleted")
})
public class WebsiteAreaEntity {
    public static final int DEFAULT_DISPLAY_ORDER = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private WebsiteCityEntity city;

    @Column(name = "area_name", nullable = false, length = 120)
    private String areaName;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = DEFAULT_DISPLAY_ORDER;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public WebsiteCityEntity getCity() { return city; }
    public void setCity(WebsiteCityEntity city) { this.city = city; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Integer getDisplayOrder() { return displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
