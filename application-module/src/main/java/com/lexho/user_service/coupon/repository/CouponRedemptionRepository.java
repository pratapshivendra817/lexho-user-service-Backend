package com.lexho.user_service.coupon.repository;

import com.lexho.user_service.coupon.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponId(Long couponId);

    long countByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponIdAndRedeemedAtBetween(Long couponId, LocalDateTime start, LocalDateTime end);

    boolean existsByBookingId(Long bookingId);

    Optional<CouponRedemption> findByBookingId(Long bookingId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
