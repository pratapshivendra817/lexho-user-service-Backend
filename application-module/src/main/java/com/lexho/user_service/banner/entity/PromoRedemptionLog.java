package com.lexho.user_service.banner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_redemption_logs")
@Getter
@Setter
public class PromoRedemptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "discount_applied", precision = 12, scale = 2)
    private BigDecimal discountApplied;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void onCreate() {
        timestamp = LocalDateTime.now();
    }
}

