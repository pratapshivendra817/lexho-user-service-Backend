package com.lexho.user_service.lexho.entity;

import com.lexho.user_service.referral.enums.ReferralStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lexho_referral_uses", indexes = {
        @Index(name = "idx_lexho_refuse_user", columnList = "user_id"),
        @Index(name = "idx_lexho_refuse_code", columnList = "code"),
        @Index(name = "idx_lexho_refuse_referrer", columnList = "referrer_user_id"),
        @Index(name = "idx_lexho_refuse_status", columnList = "status")
})
public class LexhoReferralUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "referrer_user_id", nullable = false)
    private Long referrerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralStatus status;

    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "qualifying_booking_id")
    private Long qualifyingBookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReferralStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getReferrerUserId() {
        return referrerUserId;
    }

    public void setReferrerUserId(Long referrerUserId) {
        this.referrerUserId = referrerUserId;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public LocalDateTime getQualifiedAt() {
        return qualifiedAt;
    }

    public void setQualifiedAt(LocalDateTime qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public LocalDateTime getRewardedAt() {
        return rewardedAt;
    }

    public void setRewardedAt(LocalDateTime rewardedAt) {
        this.rewardedAt = rewardedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getQualifyingBookingId() {
        return qualifyingBookingId;
    }

    public void setQualifyingBookingId(Long qualifyingBookingId) {
        this.qualifyingBookingId = qualifyingBookingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
