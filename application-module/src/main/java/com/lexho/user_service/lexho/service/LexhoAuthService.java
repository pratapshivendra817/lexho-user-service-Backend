package com.lexho.user_service.lexho.service;

import com.lexho.common.enums.Role;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoRefreshToken;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoRefreshTokenRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LexhoAuthService {

    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> throttleStore = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final LexhoUserProfileRepository profileRepository;
    private final LexhoRefreshTokenRepository refreshTokenRepository;

    @Value("${lexho.auth.otp.expiry-seconds:300}")
    private long otpExpirySeconds;

    @Value("${lexho.auth.otp.resend-after-seconds:30}")
    private long resendAfterSeconds;

    @Value("${lexho.auth.refresh-token.expiry-days:30}")
    private long refreshTokenExpiryDays;

    public LexhoAuthService(
            JwtUtil jwtUtil,
            LexhoUserProfileRepository profileRepository,
            LexhoRefreshTokenRepository refreshTokenRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String sendOtp(String mobile, String requestedRole) {
        Role role = resolveRole(requestedRole);
        String otpScope = otpScope(mobile, role);
        long now = System.currentTimeMillis();
        purgeExpired(now, otpScope);

        Long throttleUntil = throttleStore.get(otpScope);
        if (throttleUntil != null && throttleUntil > now) {
            throw new BadRequestException("Please wait before requesting OTP again");
        }

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        otpStore.put(otpScope, new OtpRecord(otp, now + (otpExpirySeconds * 1000)));
        throttleStore.put(otpScope, now + (resendAfterSeconds * 1000));
        System.out.println("Lexho OTP for " + mobile + " is: " + otp);
        return otp;
    }

    @Transactional
    public LexhoDtos.AuthVerifyResponse verifyOtp(String mobile, String otp, String requestedRole) {
        Role role = resolveRole(requestedRole);
        String otpScope = otpScope(mobile, role);
        long now = System.currentTimeMillis();
        purgeExpired(now, otpScope);

        OtpRecord record = otpStore.get(otpScope);
        if (record == null || !record.otp().equals(otp)) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        otpStore.remove(otpScope);

        LexhoUserProfile profile = profileRepository.findByMobile(mobile).orElseGet(() -> {
            LexhoUserProfile created = new LexhoUserProfile();
            created.setMobile(mobile);
            created.setName("Lexho User");
            created.setRole(role);
            return profileRepository.save(created);
        });
        if (profile.getRole() == null) {
            profile.setRole(role);
        } else if (profile.getRole() == Role.ADMIN && role != Role.ADMIN) {
            throw new BadRequestException("Role mismatch for this account");
        } else if (profile.getRole() != role) {
            // Allow OTP-auth users to switch between CUSTOMER and PARTNER using the same mobile.
            profile.setRole(role);
        }
        profileRepository.save(profile);

        String accessToken = jwtUtil.generateToken(profile.getId(), profile.getMobile(), role);
        String refreshTokenValue = UUID.randomUUID().toString() + "." + UUID.randomUUID();

        LexhoRefreshToken refreshToken = new LexhoRefreshToken();
        refreshToken.setUserId(profile.getId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new LexhoDtos.AuthVerifyResponse(
                accessToken,
                refreshTokenValue,
                new LexhoDtos.AuthUserDetails(
                        profile.getId(),
                        profile.getName(),
                        profile.getMobile(),
                        profile.getEmail(),
                        profile.getProfileImage(),
                        role.name()
                )
        );
    }

    @Transactional
    public String refreshToken(String refreshTokenValue) {
        LexhoRefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired or revoked");
        }

        LexhoUserProfile profile = profileRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        Role role = profile.getRole() == null ? Role.CUSTOMER : profile.getRole();
        return jwtUtil.generateToken(profile.getId(), profile.getMobile(), role);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public long getResendAfterSeconds() {
        return resendAfterSeconds;
    }

    private void purgeExpired(long now, String scopeKey) {
        OtpRecord record = otpStore.get(scopeKey);
        if (record != null && record.expiresAtEpochMs() <= now) {
            otpStore.remove(scopeKey);
        }
        Long throttleUntil = throttleStore.get(scopeKey);
        if (throttleUntil != null && throttleUntil <= now) {
            throttleStore.remove(scopeKey);
        }
    }

    private Role resolveRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.CUSTOMER;
        }
        try {
            Role role = Role.valueOf(requestedRole.trim().toUpperCase());
            if (role != Role.CUSTOMER && role != Role.PARTNER) {
                throw new BadRequestException("Unsupported role");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role");
        }
    }

    private String otpScope(String mobile, Role role) {
        return mobile + ":" + role.name();
    }

    private record OtpRecord(String otp, long expiresAtEpochMs) {}
}
