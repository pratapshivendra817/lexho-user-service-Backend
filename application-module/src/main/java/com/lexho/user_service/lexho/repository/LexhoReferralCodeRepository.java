package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LexhoReferralCodeRepository extends JpaRepository<LexhoReferralCode, Long> {
    Optional<LexhoReferralCode> findByUserId(Long userId);
    Optional<LexhoReferralCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
