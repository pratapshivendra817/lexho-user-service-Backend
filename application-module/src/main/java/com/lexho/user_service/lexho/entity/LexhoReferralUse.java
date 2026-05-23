package com.lexho.user_service.lexho.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lexho_referral_uses", indexes = {
        @Index(name = "idx_lexho_refuse_user", columnList = "userId"),
        @Index(name = "idx_lexho_refuse_code", columnList = "code")
})
public class LexhoReferralUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
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
}
