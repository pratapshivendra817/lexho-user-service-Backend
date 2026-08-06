package com.lexho.user_service.lexho.service;

import com.lexho.common.enums.Role;
import com.lexho.common.security.TokenDigests;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.entity.LexhoOtpChallenge;
import com.lexho.user_service.lexho.entity.LexhoRefreshToken;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoOtpChallengeRepository;
import com.lexho.user_service.lexho.repository.LexhoRefreshTokenRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LexhoAuthService {

    private static final Logger log = LoggerFactory.getLogger(LexhoAuthService.class);

    private final JwtUtil jwtUtil;
    private final LexhoUserProfileRepository profileRepository;
    private final LexhoRefreshTokenRepository refreshTokenRepository;
    private final LexhoOtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${lexho.auth.otp.expiry-seconds:300}")
    private long otpExpirySeconds;

    @Value("${lexho.auth.otp.resend-after-seconds:30}")
    private long resendAfterSeconds;

    @Value("${lexho.auth.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${lexho.auth.refresh-token.expiry-days:30}")
    private long refreshTokenExpiryDays;

    @Value("${lexho.auth.otp.log-to-console:false}")
    private boolean logOtpToConsole;

    public LexhoAuthService(
            JwtUtil jwtUtil,
            LexhoUserProfileRepository profileRepository,
            LexhoRefreshTokenRepository refreshTokenRepository,
            LexhoOtpChallengeRepository otpChallengeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtUtil = jwtUtil;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String sendOtp(String mobile, String requestedRole) {
        Role role = resolveRole(requestedRole);
        LocalDateTime now = LocalDateTime.now();
        otpChallengeRepository.findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc(mobile, role.name())
                .ifPresent(existing -> {
                    if (existing.getExpiresAt().isAfter(now)
                            && existing.getCreatedAt().plusSeconds(resendAfterSeconds).isAfter(now)) {
                        throw new BadRequestException("Please wait before requesting OTP again");
                    }
                    if (existing.getExpiresAt().isBefore(now) || existing.getAttempts() >= otpMaxAttempts) {
                        existing.setConsumedAt(now);
                        otpChallengeRepository.save(existing);
                    }
                });

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        LexhoOtpChallenge challenge = new LexhoOtpChallenge();
        challenge.setMobile(mobile);
        challenge.setRole(role.name());
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(now.plusSeconds(otpExpirySeconds));
        challenge.setAttempts(0);
        otpChallengeRepository.save(challenge);
        if (logOtpToConsole) {
            log.info("Lexho OTP for mobile={} role={} otp={}", mobile, role.name(), otp);
        }
        return otp;
    }

    @Transactional
    public LexhoDtos.AuthVerifyResponse verifyOtp(String mobile, String otp, String requestedRole) {
        Role role = resolveRole(requestedRole);
        LocalDateTime now = LocalDateTime.now();
        LexhoOtpChallenge challenge = otpChallengeRepository
                .findTopByMobileAndRoleAndConsumedAtIsNullOrderByCreatedAtDesc(mobile, role.name())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (challenge.getExpiresAt().isBefore(now)) {
            challenge.setConsumedAt(now);
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Invalid or expired OTP");
        }
        if (challenge.getAttempts() >= otpMaxAttempts) {
            challenge.setConsumedAt(now);
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Maximum OTP attempts exceeded");
        }
        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= otpMaxAttempts) {
                challenge.setConsumedAt(now);
                otpChallengeRepository.save(challenge);
                throw new BadRequestException("Maximum OTP attempts exceeded");
            }
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Invalid or expired OTP");
        }

        challenge.setConsumedAt(now);
        otpChallengeRepository.save(challenge);

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
            profile.setRole(role);
        }
        profileRepository.save(profile);

        String accessToken = jwtUtil.generateToken(profile.getId(), profile.getMobile(), role);
        String refreshTokenValue = UUID.randomUUID() + "." + UUID.randomUUID();

        LexhoRefreshToken refreshToken = new LexhoRefreshToken();
        refreshToken.setUserId(profile.getId());
        refreshToken.setToken(hashRefreshToken(refreshTokenValue));
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
        LexhoRefreshToken refreshToken = refreshTokenRepository.findByToken(hashRefreshToken(refreshTokenValue))
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
        refreshTokenRepository.findByToken(hashRefreshToken(refreshTokenValue)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public long getResendAfterSeconds() {
        return resendAfterSeconds;
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

    private String hashRefreshToken(String refreshTokenValue) {
        return TokenDigests.sha256Hex(refreshTokenValue);
    }
}
