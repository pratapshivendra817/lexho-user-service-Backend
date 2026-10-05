package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LexhoOtpChallengeRepository extends JpaRepository<LexhoOtpChallenge, Long> {
    Optional<LexhoOtpChallenge> findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc(String mobile, String role);
}
