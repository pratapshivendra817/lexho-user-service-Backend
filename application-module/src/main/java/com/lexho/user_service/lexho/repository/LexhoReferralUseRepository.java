package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoReferralUse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LexhoReferralUseRepository extends JpaRepository<LexhoReferralUse, Long> {
    boolean existsByUserIdAndCodeIgnoreCase(Long userId, String code);

    boolean existsByUserId(Long userId);

    Optional<LexhoReferralUse> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LexhoReferralUse r where r.userId = :userId")
    Optional<LexhoReferralUse> findByUserIdForUpdate(@Param("userId") Long userId);
}
