package com.lexho.user_service.homebanner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "home_banners", indexes = {
        @Index(name = "idx_home_banners_active_priority", columnList = "is_active,priority")
})
@Getter
@Setter
public class HomeBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "redirect_type", nullable = false, length = 20)
    private HomeBannerRedirectType redirectType;

    @Column(name = "redirect_id")
    private Long redirectId;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "banner_type", length = 50)
    private String bannerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 20)
    private HomeBannerAudience audience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.priority == null) {
            this.priority = 100;
        }
        if (this.redirectType == null) {
            this.redirectType = HomeBannerRedirectType.NONE;
        }
        if (this.audience == null) {
            this.audience = HomeBannerAudience.BOTH;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
