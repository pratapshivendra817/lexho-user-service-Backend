package com.lexho.user_service.service;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.admin.entity.AdminOtp;
import com.lexho.user_service.admin.repository.AdminOtpRepository;
import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdminOtpService {

    private static final Logger log = LoggerFactory.getLogger(AdminOtpService.class);
    private static final String OTP_HASH_FIELD = "otpHash";
    private static final String ATTEMPTS_FIELD = "attempts";

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuthProperties adminAuthProperties;
    private final AdminOtpRepository adminOtpRepository;
    private final AdminRepository adminRepository;
    private final Map<String, LocalOtpRecord> localOtpStore = new ConcurrentHashMap<>();

    public AdminOtpService(StringRedisTemplate stringRedisTemplate,
                           PasswordEncoder passwordEncoder,
                           AdminAuthProperties adminAuthProperties,
                           AdminOtpRepository adminOtpRepository,
                           AdminRepository adminRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminAuthProperties = adminAuthProperties;
        this.adminOtpRepository = adminOtpRepository;
        this.adminRepository = adminRepository;
    }

    public String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    public void storeOtp(String email, Long adminId, String otp) {
        String normalizedEmail = normalizeEmail(email);
        String key = otpKey(normalizedEmail);
        String otpHash = passwordEncoder.encode(otp);
        int expirySeconds = adminAuthProperties.getOtp().getExpirySeconds();
        Duration expiry = Duration.ofSeconds(expirySeconds);

        try {
            HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
            Map<String, String> payload = new HashMap<>();
            payload.put(OTP_HASH_FIELD, otpHash);
            payload.put(ATTEMPTS_FIELD, "0");

            hashOperations.putAll(key, payload);
            stringRedisTemplate.expire(key, expiry);
        } catch (RuntimeException ex) {
            if (!adminAuthProperties.getInfrastructure().isAllowInMemoryOtpFallback()) {
                storeOtpInDbFallback(adminId, otpHash, expirySeconds);
                return;
            }
            log.warn("Redis unavailable while storing admin OTP. Falling back to in-memory store");
            localOtpStore.put(key, new LocalOtpRecord(otpHash, 0, System.currentTimeMillis() + expiry.toMillis()));
        }
    }

    public void validateOtp(String email, Long adminId, String otp) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOtp = otp == null ? "" : otp.trim();
        if (normalizedOtp.isEmpty()) {
            throw new BadRequestException("OTP is required");
        }

        String key = otpKey(normalizedEmail);
        try {
            HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
            Object otpHashValue = hashOperations.get(key, OTP_HASH_FIELD);
            Object attemptsValue = hashOperations.get(key, ATTEMPTS_FIELD);

            if (otpHashValue == null) {
                throw new BadRequestException("OTP not found or expired");
            }

            int attempts = parseAttempts(attemptsValue);
            int maxAttempts = adminAuthProperties.getOtp().getMaxAttempts();
            if (attempts >= maxAttempts) {
                stringRedisTemplate.delete(key);
                throw new BadRequestException("Maximum OTP attempts exceeded");
            }

            String otpHash = String.valueOf(otpHashValue);
            if (!passwordEncoder.matches(normalizedOtp, otpHash)) {
                attempts += 1;
                hashOperations.put(key, ATTEMPTS_FIELD, String.valueOf(attempts));
                if (attempts >= maxAttempts) {
                    stringRedisTemplate.delete(key);
                    throw new BadRequestException("Maximum OTP attempts exceeded");
                }
                throw new BadRequestException("Invalid OTP");
            }
        } catch (RuntimeException ex) {
            if (ex instanceof BadRequestException) {
                throw ex;
            }
            if (!adminAuthProperties.getInfrastructure().isAllowInMemoryOtpFallback()) {
                validateOtpFromDbFallback(adminId, normalizedOtp);
                return;
            }
            log.warn("Redis unavailable while validating admin OTP. Falling back to in-memory store");
            validateOtpFromLocalStore(key, normalizedOtp);
        }
    }

    private void validateOtpFromLocalStore(String key, String normalizedOtp) {
        LocalOtpRecord otpRecord = localOtpStore.get(key);
        if (otpRecord == null || otpRecord.expiresAtEpochMs < System.currentTimeMillis()) {
            localOtpStore.remove(key);
            throw new BadRequestException("OTP not found or expired");
        }

        int maxAttempts = adminAuthProperties.getOtp().getMaxAttempts();
        if (otpRecord.attempts >= maxAttempts) {
            localOtpStore.remove(key);
            throw new BadRequestException("Maximum OTP attempts exceeded");
        }

        if (!passwordEncoder.matches(normalizedOtp, otpRecord.otpHash)) {
            otpRecord.attempts += 1;
            if (otpRecord.attempts >= maxAttempts) {
                localOtpStore.remove(key);
                throw new BadRequestException("Maximum OTP attempts exceeded");
            }
            throw new BadRequestException("Invalid OTP");
        }
    }

    public void deleteOtp(String email, Long adminId) {
        String key = otpKey(normalizeEmail(email));
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException ex) {
            if (!adminAuthProperties.getInfrastructure().isAllowInMemoryOtpFallback()) {
                consumeLatestDbOtp(adminId);
                return;
            }
            log.warn("Redis unavailable while deleting admin OTP. Removing from in-memory store");
            localOtpStore.remove(key);
        }
    }

    private void storeOtpInDbFallback(Long adminId, String otpHash, int expirySeconds) {
        if (adminId == null) {
            throw new InfrastructureException("OTP service is temporarily unavailable");
        }
        com.lexho.admin.entity.Admin adminRef = adminRepository.findById(adminId)
                .orElseThrow(() -> new InfrastructureException("OTP service is temporarily unavailable"));
        AdminOtp adminOtp = new AdminOtp();
        adminOtp.setAdmin(adminRef);
        adminOtp.setOtpCode(otpHash);
        adminOtp.setExpiresAt(LocalDateTime.now().plusSeconds(expirySeconds));
        adminOtp.setAttempts(0);
        adminOtp.setConsumedAt(null);
        adminOtpRepository.save(adminOtp);
    }

    private void validateOtpFromDbFallback(Long adminId, String otp) {
        if (adminId == null) {
            throw new InfrastructureException("OTP service is temporarily unavailable");
        }
        AdminOtp adminOtp = adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(adminId)
                .orElseThrow(() -> new BadRequestException("OTP not found or expired"));
        if (adminOtp.getConsumedAt() != null || LocalDateTime.now().isAfter(adminOtp.getExpiresAt())) {
            throw new BadRequestException("OTP not found or expired");
        }
        int maxAttempts = adminAuthProperties.getOtp().getMaxAttempts();
        if (adminOtp.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Maximum OTP attempts exceeded");
        }
        if (!passwordEncoder.matches(otp, adminOtp.getOtpCode())) {
            adminOtp.setAttempts(adminOtp.getAttempts() + 1);
            adminOtpRepository.save(adminOtp);
            if (adminOtp.getAttempts() >= maxAttempts) {
                throw new BadRequestException("Maximum OTP attempts exceeded");
            }
            throw new BadRequestException("Invalid OTP");
        }
        adminOtp.setConsumedAt(LocalDateTime.now());
        adminOtpRepository.save(adminOtp);
    }

    private void consumeLatestDbOtp(Long adminId) {
        if (adminId == null) {
            return;
        }
        adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(adminId).ifPresent(adminOtp -> {
            adminOtp.setConsumedAt(LocalDateTime.now());
            adminOtpRepository.save(adminOtp);
        });
    }

    private int parseAttempts(Object attemptsValue) {
        if (attemptsValue == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(attemptsValue));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String otpKey(String normalizedEmail) {
        return adminAuthProperties.getOtp().getKeyPrefix() + normalizedEmail;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static class LocalOtpRecord {
        private final String otpHash;
        private int attempts;
        private final long expiresAtEpochMs;

        private LocalOtpRecord(String otpHash, int attempts, long expiresAtEpochMs) {
            this.otpHash = otpHash;
            this.attempts = attempts;
            this.expiresAtEpochMs = expiresAtEpochMs;
        }
    }
}
