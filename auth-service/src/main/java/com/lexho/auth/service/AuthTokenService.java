package com.lexho.auth.service;

import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.entity.RefreshToken;
import com.lexho.auth.repository.RefreshTokenRepository;
import com.lexho.auth.security.JwtTokenService;
import com.lexho.auth.support.AuthRequestSupport;
import com.lexho.common.enums.Role;
import com.lexho.common.security.TokenDigests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthTokenService {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiryDays;

    public AuthTokenService(JwtTokenService jwtTokenService,
                            RefreshTokenRepository refreshTokenRepository,
                            @Value("${security.jwt.refresh-expiry-days:30}") long refreshExpiryDays) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    @Transactional
    public TokenPairResponse issueTokens(Long userId, Role role) {
        return new TokenPairResponse(
                jwtTokenService.generateAccessToken(userId, role),
                persistRefreshToken(userId, role)
        );
    }

    @Transactional
    public TokenPairResponse refreshTokens(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(hashRefreshToken(refreshTokenValue))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        revoke(token);
        return issueTokens(token.getUserId(), AuthRequestSupport.parseRole(token.getRole()));
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(hashRefreshToken(refreshTokenValue))
                .ifPresent(this::revoke);
    }

    private String persistRefreshToken(Long userId, Role role) {
        String value = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setRole(role.name());
        token.setToken(hashRefreshToken(value));
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshExpiryDays));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return value;
    }

    private void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private String hashRefreshToken(String refreshTokenValue) {
        return TokenDigests.sha256Hex(AuthRequestSupport.normalizeRefreshToken(refreshTokenValue));
    }
}
