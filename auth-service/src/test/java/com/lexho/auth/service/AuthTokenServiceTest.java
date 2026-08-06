package com.lexho.auth.service;

import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.entity.RefreshToken;
import com.lexho.auth.repository.RefreshTokenRepository;
import com.lexho.auth.security.JwtTokenService;
import com.lexho.common.enums.Role;
import com.lexho.common.security.TokenDigests;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final AuthTokenService service = new AuthTokenService(jwtTokenService, refreshTokenRepository, 30);

    @Test
    void issueTokensStoresHashedRefreshTokenButReturnsRawToken() {
        when(jwtTokenService.generateAccessToken(7L, Role.CUSTOMER)).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPairResponse issued = service.issueTokens(7L, Role.CUSTOMER);

        assertThat(issued.getAccessToken()).isEqualTo("access");
        assertThat(issued.getRefreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAndRevokeLookupByDigest() {
        RefreshToken token = new RefreshToken();
        token.setUserId(9L);
        token.setRole(Role.PARTNER.name());
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken(TokenDigests.sha256Hex("raw-token")))
                .thenReturn(Optional.of(token));
        when(jwtTokenService.generateAccessToken(9L, Role.PARTNER)).thenReturn("next-access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPairResponse refreshed = service.refreshTokens("raw-token");
        assertThat(refreshed.getAccessToken()).isEqualTo("next-access");
        assertThat(token.isRevoked()).isTrue();

        token.setRevoked(false);
        service.revokeRefreshToken("raw-token");
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(refreshTokenRepository.findByToken(TokenDigests.sha256Hex("missing")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshTokens("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
