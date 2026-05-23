package com.lexho.auth.service;

import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.entity.RefreshToken;
import com.lexho.auth.repository.RefreshTokenRepository;
import com.lexho.auth.security.JwtTokenService;
import com.lexho.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtTokenService jwtTokenService;
    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                "this-is-a-very-long-test-secret-with-min-32-bytes",
                60_000,
                "test-issuer"
        );
        authTokenService = new AuthTokenService(jwtTokenService, refreshTokenRepository, 30);
    }

    @Test
    void shouldIssueAccessAndRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPairResponse response = authTokenService.issueTokens(100L, Role.ADMIN);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals("ADMIN", captor.getValue().getRole());
    }

    @Test
    void shouldRefreshTokenAndRevokeOldOne() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUserId(200L);
        oldToken.setRole(Role.CUSTOMER.name());
        oldToken.setToken("old-refresh");
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        oldToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPairResponse response = authTokenService.refreshTokens("old-refresh");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertTrue(oldToken.isRevoked());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setUserId(1L);
        token.setRole(Role.ADMIN.name());
        token.setToken("expired");
        token.setExpiresAt(LocalDateTime.now().minusSeconds(5));
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> authTokenService.refreshTokens("expired"));
    }
}
