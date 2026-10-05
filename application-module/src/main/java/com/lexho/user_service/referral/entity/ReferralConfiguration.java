package com.lexho.user_service.referral.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_config")
@Getter
@Setter
public class ReferralConfiguration {
    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "referrer_reward_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal referrerRewardAmount;

    @Column(name = "referred_user_reward_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal referredUserRewardAmount;

    @Column(name = "minimum_qualifying_purchase_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumQualifyingPurchaseAmount;

    @Column(name = "qualification_condition", nullable = false, length = 40)
    private String qualificationCondition;

    @Column(name = "expiry_days")
    private Integer expiryDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (referrerRewardAmount == null) {
            referrerRewardAmount = BigDecimal.ZERO;
        }
        if (referredUserRewardAmount == null) {
            referredUserRewardAmount = BigDecimal.ZERO;
        }
        if (minimumQualifyingPurchaseAmount == null) {
            minimumQualifyingPurchaseAmount = BigDecimal.ZERO;
        }
        if (qualificationCondition == null || qualificationCondition.isBlank()) {
            qualificationCondition = "SUCCESSFUL_PAYMENT";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
