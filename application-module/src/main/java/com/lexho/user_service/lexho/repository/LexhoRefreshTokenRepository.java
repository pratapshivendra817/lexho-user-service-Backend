package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LexhoRefreshTokenRepository extends JpaRepository<LexhoRefreshToken, Long> {
    Optional<LexhoRefreshToken> findByToken(String token);
}

