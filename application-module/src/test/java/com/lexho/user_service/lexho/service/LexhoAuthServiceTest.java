package com.lexho.user_service.lexho.service;

import com.lexho.common.enums.Role;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoOtpChallenge;
import com.lexho.user_service.lexho.entity.LexhoRefreshToken;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoOtpChallengeRepository;
import com.lexho.user_service.lexho.repository.LexhoRefreshTokenRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final LexhoOtpChallengeRepository otpChallenges = mock(LexhoOtpChallengeRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LexhoAuthService service =
            new LexhoAuthService(jwt, profiles, refreshTokens, otpChallenges, passwordEncoder);

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "otpExpirySeconds", 300L);
        ReflectionTestUtils.setField(service, "resendAfterSeconds", 30L);
        ReflectionTestUtils.setField(service, "otpMaxAttempts", 5);
        ReflectionTestUtils.setField(service, "refreshTokenExpiryDays", 30L);
        ReflectionTestUtils.setField(service, "logOtpToConsole", false);
        when(otpChallenges.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendsVerifiesOtpCreatesProfileAndStoresHashedRefreshToken() {
        when(otpChallenges.findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc("9999999999", "CUSTOMER"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(activeChallenge("9999999999", Role.CUSTOMER, "otp-hash")));

        when(passwordEncoder.encode(any())).thenReturn("otp-hash");
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(profiles.findByMobile("9999999999")).thenReturn(Optional.empty());
        when(profiles.save(any())).thenAnswer(invocation -> {
            LexhoUserProfile profile = invocation.getArgument(0);
            ReflectionTestUtils.setField(profile, "id", 7L);
            return profile;
        });
        when(jwt.generateToken(7L, "9999999999", Role.CUSTOMER)).thenReturn("access");

        String otp = service.sendOtp("9999999999", null);
        var response = service.verifyOtp("9999999999", otp, "customer");

        assertThat(response.token()).isEqualTo("access");
        assertThat(response.user().role()).isEqualTo("CUSTOMER");
        verify(refreshTokens).save(any(LexhoRefreshToken.class));
    }

    @Test
    void blocksRapidResendAndExpiredOrExhaustedChallenges() {
        LexhoOtpChallenge rapid = activeChallenge("9999999999", Role.CUSTOMER, "otp-hash");
        ReflectionTestUtils.setField(rapid, "createdAt", LocalDateTime.now());
        when(otpChallenges.findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc("9999999999", "CUSTOMER"))
                .thenReturn(Optional.of(rapid));

        assertThatThrownBy(() -> service.sendOtp("9999999999", "customer"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("wait");

        LexhoOtpChallenge expired = activeChallenge("8888888888", Role.CUSTOMER, "otp-hash");
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(otpChallenges.findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc("8888888888", "CUSTOMER"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verifyOtp("8888888888", "123456", "customer"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validatesRolesAndRefreshTokenStates() {
        assertThatThrownBy(() -> service.verifyOtp("1", "bad", "partner")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.sendOtp("1", "ADMIN")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.sendOtp("1", "bad")).isInstanceOf(BadRequestException.class);

        when(refreshTokens.findByToken(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refreshToken("missing")).isInstanceOf(BadRequestException.class);

        LexhoRefreshToken expired = new LexhoRefreshToken();
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokens.findByToken(any())).thenReturn(Optional.of(expired));
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
        when(refreshTokens.findByToken(any())).thenReturn(Optional.of(token));
        when(profiles.findById(8L)).thenReturn(Optional.of(profile));
        when(jwt.generateToken(8L, "8888888888", Role.CUSTOMER)).thenReturn("new-access");

        assertThat(service.refreshToken("refresh")).isEqualTo("new-access");
        service.logout("refresh");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens).save(token);
        assertThat(service.getResendAfterSeconds()).isEqualTo(30);
    }

    private LexhoOtpChallenge activeChallenge(String mobile, Role role, String otpHash) {
        LexhoOtpChallenge challenge = new LexhoOtpChallenge();
        challenge.setMobile(mobile);
        challenge.setRole(role.name());
        challenge.setOtpHash(otpHash);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setAttempts(0);
        ReflectionTestUtils.setField(challenge, "createdAt", LocalDateTime.now().minusMinutes(1));
        return challenge;
    }
}
