package com.lexho.user_service.referral.repository;

import com.lexho.user_service.referral.entity.ReferralRewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralRewardTransactionRepository extends JpaRepository<ReferralRewardTransaction, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<ReferralRewardTransaction> findByIdempotencyKey(String idempotencyKey);
}
