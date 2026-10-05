package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoReferralCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LexhoReferralCodeRepository extends JpaRepository<LexhoReferralCode, Long> {
    Optional<LexhoReferralCode> findByUserId(Long userId);
    Optional<LexhoReferralCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LexhoReferralCode c where upper(c.code) = upper(:code)")
    Optional<LexhoReferralCode> findByCodeForUpdate(@Param("code") String code);
}
