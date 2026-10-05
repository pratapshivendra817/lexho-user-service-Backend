package com.lexho.user_service.website.entity;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "website_category_config", uniqueConstraints = {
        @UniqueConstraint(name = "uk_website_category_config_category", columnNames = "category_id")
})
public class WebsiteCategoryConfig {
    public static final int DEFAULT_DISPLAY_ORDER = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AdminCategoryEntity category;

    @Column(name = "show_on_website", nullable = false)
    private boolean showOnWebsite;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = DEFAULT_DISPLAY_ORDER;

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
    public AdminCategoryEntity getCategory() { return category; }
    public void setCategory(AdminCategoryEntity category) { this.category = category; }
    public boolean isShowOnWebsite() { return showOnWebsite; }
    public void setShowOnWebsite(boolean showOnWebsite) { this.showOnWebsite = showOnWebsite; }
    public Integer getDisplayOrder() { return displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
