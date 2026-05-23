package com.lexho.user_service.customer.banner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_banners", indexes = {
        @Index(name = "idx_customer_banners_category_id", columnList = "category_id"),
        @Index(name = "idx_customer_banners_active_priority", columnList = "is_active,priority")
})
@Getter
@Setter
public class CustomerBannerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "redirect_type", nullable = false, length = 20)
    private BannerRedirectType redirectType;

    @Column(name = "redirect_id")
    private Long redirectId;

    @Column(name = "redirect_url", length = 1000)
    private String redirectUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.priority == null) {
            this.priority = 100;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
