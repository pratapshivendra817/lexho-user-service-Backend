package com.lexho.user_service.coupon.entity;

import com.lexho.user_service.coupon.enums.CouponDiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "minimum_purchase_amount", precision = 12, scale = 2)
    private BigDecimal minimumPurchaseAmount;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_usage_limit")
    private Integer perUserUsageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible = true;

    @Column(name = "first_booking_only", nullable = false)
    private boolean firstBookingOnly;

    @Column(name = "applicable_service_id")
    private Long applicableServiceId;

    @Column(name = "applicable_category_id")
    private Long applicableCategoryId;

    @Column(name = "applicable_booking_type", length = 30)
    private String applicableBookingType;

    @Column(name = "max_uses_per_day")
    private Integer maxUsesPerDay;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_reference_id", length = 80)
    private String sourceReferenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (usedCount == null) {
            usedCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
