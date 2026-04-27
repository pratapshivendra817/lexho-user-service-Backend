package com.lexho.user_service.service;

import com.lexho.auth.entity.Otp;
import com.lexho.auth.repository.OtpRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_AFTER_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    public OtpService(OtpRepository otpRepository, PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateOtp(String mobile) {
        String normalizedMobile = normalizeMobile(mobile);
        LocalDateTime now = LocalDateTime.now();
        List<Otp> pendingOtps = otpRepository.findByMobileAndStatus(normalizedMobile, STATUS_PENDING);

        for (Otp pendingOtp : pendingOtps) {
            if (pendingOtp.getCreatedAt() != null
                    && pendingOtp.getCreatedAt().plusSeconds(RESEND_AFTER_SECONDS).isAfter(now)) {
                throw new BadRequestException("Please wait before requesting OTP again");
            }
            pendingOtp.setStatus(STATUS_EXPIRED);
        }

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        Otp otpEntity = new Otp();
        otpEntity.setMobile(normalizedMobile);
        otpEntity.setOtpHash(passwordEncoder.encode(otp));
        otpEntity.setStatus(STATUS_PENDING);
        otpEntity.setAttempts(0);
        otpEntity.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));

        otpRepository.save(otpEntity);
        System.out.println("Generated OTP for " + normalizedMobile + ": " + otp);
    }

    @Transactional
    public void verifyOtp(String mobile, String enteredOtp) {
        String normalizedMobile = normalizeMobile(mobile);
        String normalizedOtp = enteredOtp == null ? "" : enteredOtp.trim();

        if (normalizedOtp.isEmpty()) {
            throw new BadRequestException("OTP is required");
        }

        Otp otp = otpRepository.findTopByMobileOrderByIdDesc(normalizedMobile)
                .orElseThrow(() -> new BadRequestException("OTP not found"));

        if (!STATUS_PENDING.equals(otp.getStatus())) {
            throw new BadRequestException("OTP is no longer valid");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(STATUS_EXPIRED);
            throw new BadRequestException("OTP expired");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setStatus(STATUS_FAILED);
            throw new BadRequestException("Maximum OTP attempts exceeded");
        }

        if (!passwordEncoder.matches(normalizedOtp, otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                otp.setStatus(STATUS_FAILED);
            }
            throw new BadRequestException("Invalid OTP");
        }

        otp.setAttempts(otp.getAttempts() + 1);
        otp.setStatus(STATUS_VERIFIED);
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            throw new BadRequestException("Mobile number is required");
        }
        return mobile.trim();
    }
}

