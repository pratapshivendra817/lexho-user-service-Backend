package com.lexho.user_service.lexho.service;

import com.lexho.common.enums.Role;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoRefreshToken;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoRefreshTokenRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexhoAuthServiceTest {

    private final JwtUtil jwt = mock(JwtUtil.class);
    private final LexhoUserProfileRepository profiles = mock(LexhoUserProfileRepository.class);
    private final LexhoRefreshTokenRepository refreshTokens = mock(LexhoRefreshTokenRepository.class);
    private final LexhoAuthService service = new LexhoAuthService(jwt, profiles, refreshTokens);

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "otpExpirySeconds", 300L);
        ReflectionTestUtils.setField(service, "resendAfterSeconds", 30L);
        ReflectionTestUtils.setField(service, "refreshTokenExpiryDays", 30L);
        ReflectionTestUtils.setField(service, "logOtpToConsole", true);
    }

    @Test
    void sendsVerifiesOtpCreatesProfileAndTokens() {
        String otp = service.sendOtp("9999999999", null);
        when(profiles.findByMobile("9999999999")).thenReturn(Optional.empty());
        when(profiles.save(any())).thenAnswer(invocation -> {
            LexhoUserProfile profile = invocation.getArgument(0);
            ReflectionTestUtils.setField(profile, "id", 7L);
            return profile;
        });
        when(jwt.generateToken(7L, "9999999999", Role.CUSTOMER)).thenReturn("access");

        var response = service.verifyOtp("9999999999", otp, "customer");

        assertThat(response.token()).isEqualTo("access");
        assertThat(response.user().role()).isEqualTo("CUSTOMER");
        verify(refreshTokens).save(any(LexhoRefreshToken.class));
        assertThatThrownBy(() -> service.sendOtp("9999999999", "customer"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("wait");
    }

    @Test
    void validatesOtpRolesAndRefreshTokenStates() {
        assertThatThrownBy(() -> service.verifyOtp("1", "bad", "partner")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.sendOtp("1", "ADMIN")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.sendOtp("1", "bad")).isInstanceOf(BadRequestException.class);

        when(refreshTokens.findByToken("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refreshToken("missing")).isInstanceOf(BadRequestException.class);

        LexhoRefreshToken expired = new LexhoRefreshToken();
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokens.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.refreshToken("expired")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void refreshesWithDefaultRoleAndRevokesOnLogout() {
        LexhoRefreshToken token = new LexhoRefreshToken();
        token.setUserId(8L);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        LexhoUserProfile profile = new LexhoUserProfile();
        ReflectionTestUtils.setField(profile, "id", 8L);
        profile.setMobile("8888888888");
        when(refreshTokens.findByToken("refresh")).thenReturn(Optional.of(token));
        when(profiles.findById(8L)).thenReturn(Optional.of(profile));
        when(jwt.generateToken(8L, "8888888888", Role.CUSTOMER)).thenReturn("new-access");

        assertThat(service.refreshToken("refresh")).isEqualTo("new-access");
        service.logout("refresh");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens).save(token);
        assertThat(service.getResendAfterSeconds()).isEqualTo(30);
    }
}
