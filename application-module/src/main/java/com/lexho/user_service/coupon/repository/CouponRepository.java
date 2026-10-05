package com.lexho.user_service.coupon.repository;

import com.lexho.user_service.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);

    Optional<Coupon> findBySourceTypeAndSourceReferenceId(String sourceType, String sourceReferenceId);

    boolean existsByCodeIgnoreCase(String code);

    Page<Coupon> findByActive(boolean active, Pageable pageable);

    Page<Coupon> findByAssignedUserIdAndSourceType(Long assignedUserId, String sourceType, Pageable pageable);

    @Query("""
            select c from Coupon c
            where c.active = true
              and c.customerVisible = true
              and (c.validFrom is null or c.validFrom <= :now)
              and (c.validUntil is null or c.validUntil >= :now)
              and (c.assignedUserId is null or c.assignedUserId = :userId)
              and (c.totalUsageLimit is null or c.usedCount < c.totalUsageLimit)
            """)
    List<Coupon> findCustomerVisibleCandidates(@Param("userId") Long userId,
                                               @Param("now") LocalDateTime now,
                                               Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where upper(c.code) = upper(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
