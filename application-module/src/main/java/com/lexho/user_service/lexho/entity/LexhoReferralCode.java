package com.lexho.user_service.lexho.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lexho_referral_codes", indexes = {
        @Index(name = "idx_lexho_ref_user", columnList = "userId"),
        @Index(name = "idx_lexho_ref_code", columnList = "code", unique = true)
})
public class LexhoReferralCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40, unique = true)
    private String code;

    @Column(nullable = false)
    private Integer usedCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.usedCount == null) {
            this.usedCount = 0;
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

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
}
