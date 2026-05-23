package com.lexho.user_service.banner.entity;

import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "banner_promotions", indexes = {
        @Index(name = "idx_banner_promo_status", columnList = "status"),
        @Index(name = "idx_banner_promo_city", columnList = "city"),
        @Index(name = "idx_banner_promo_slot", columnList = "slot_number"),
        @Index(name = "idx_banner_promo_start_date", columnList = "start_date"),
        @Index(name = "idx_banner_promo_end_date", columnList = "end_date")
})
@Getter
@Setter
public class BannerPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(length = 1000)
    private String description;

    @Column(name = "banner_image_url", nullable = false, length = 1000)
    private String bannerImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 20)
    private BannerPromoType promoType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BannerStatus status;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(name = "coupon_code", unique = true, length = 80)
    private String couponCode;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(length = 100)
    private String city;

    @Column(name = "new_user_only", nullable = false)
    private Boolean newUserOnly;

    @Column(name = "premium_user_only", nullable = false)
    private Boolean premiumUserOnly;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false, length = 20)
    private BannerPlatformType platformType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BannerPlacement placement;

    @Enumerated(EnumType.STRING)
    @Column(name = "redirect_type", nullable = false, length = 30)
    private BannerRedirectType redirectType;

    @Column(name = "redirect_value", length = 1000)
    private String redirectValue;

    @Column(name = "click_count", nullable = false)
    private Long clickCount;

    @Column(name = "impression_count", nullable = false)
    private Long impressionCount;

    @Column(name = "redemption_count", nullable = false)
    private Long redemptionCount;

    @Column(name = "revenue_generated", nullable = false, precision = 14, scale = 2)
    private BigDecimal revenueGenerated;

    @Column(name = "max_views_per_user_per_day")
    private Integer maxViewsPerUserPerDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = BannerStatus.DRAFT;
        }
        if (platformType == null) {
            platformType = BannerPlatformType.ALL;
        }
        if (placement == null) {
            placement = BannerPlacement.TOP;
        }
        if (redirectType == null) {
            redirectType = BannerRedirectType.NONE;
        }
        if (newUserOnly == null) {
            newUserOnly = false;
        }
        if (premiumUserOnly == null) {
            premiumUserOnly = false;
        }
        if (clickCount == null) {
            clickCount = 0L;
        }
        if (impressionCount == null) {
            impressionCount = 0L;
        }
        if (redemptionCount == null) {
            redemptionCount = 0L;
        }
        if (revenueGenerated == null) {
            revenueGenerated = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
