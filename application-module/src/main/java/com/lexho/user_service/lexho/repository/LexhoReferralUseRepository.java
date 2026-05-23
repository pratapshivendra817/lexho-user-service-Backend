package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoReferralUse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LexhoReferralUseRepository extends JpaRepository<LexhoReferralUse, Long> {
    boolean existsByUserIdAndCodeIgnoreCase(Long userId, String code);
}
