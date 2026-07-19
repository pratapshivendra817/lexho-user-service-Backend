package com.lexho.user_service.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.admin.entity.AdminOtp;
import com.lexho.user_service.admin.repository.AdminOtpRepository;
import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.InfrastructureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOtpServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AdminOtpRepository adminOtpRepository;
    @Mock AdminRepository adminRepository;

    private AdminAuthProperties properties;
    private AdminOtpService service;

    @BeforeEach
    void setUp() {
        properties = new AdminAuthProperties();
        properties.getOtp().setKeyPrefix("OTP:");
        properties.getOtp().setExpirySeconds(120);
        properties.getOtp().setMaxAttempts(3);
        service = new AdminOtpService(redisTemplate, passwordEncoder, properties, adminOtpRepository, adminRepository);
    }

    @Test
    void generateOtpShouldReturnSixDigits() {
        assertTrue(service.generateOtp().matches("[0-9]{6}"));
    }

    @Test
    void storeOtpShouldNormalizeEmailHashAndExpireRedisKey() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(passwordEncoder.encode("123456")).thenReturn("hash");

        service.storeOtp(" ADMIN@Example.COM ", 7L, "123456");

        verify(hashOperations).putAll(eq("OTP:admin@example.com"),
                argThat(payload -> "hash".equals(payload.get("otpHash")) && "0".equals(payload.get("attempts"))));
        verify(redisTemplate).expire("OTP:admin@example.com", Duration.ofSeconds(120));
    }

    @Test
    void validateOtpShouldAcceptMatchingRedisHash() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("OTP:admin@example.com", "otpHash")).thenReturn("hash");
        when(hashOperations.get("OTP:admin@example.com", "attempts")).thenReturn("bad-number");
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertDoesNotThrow(() -> service.validateOtp("Admin@Example.com", 7L, " 123456 "));
        verify(hashOperations, never()).put(anyString(), any(), any());
    }

    @Test
    void validateOtpShouldIncrementAttemptsAndDeleteAtLimit() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("OTP:admin@example.com", "otpHash")).thenReturn("hash");
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);
        when(hashOperations.get("OTP:admin@example.com", "attempts")).thenReturn("1", "2");

        assertEquals("Invalid OTP", assertThrows(BadRequestException.class,
                () -> service.validateOtp("admin@example.com", 7L, "000000")).getMessage());
        assertEquals("Maximum OTP attempts exceeded", assertThrows(BadRequestException.class,
                () -> service.validateOtp("admin@example.com", 7L, "000000")).getMessage());
        verify(redisTemplate).delete("OTP:admin@example.com");
    }

    @Test
    void validateOtpShouldRejectMissingExpiredAndBlankOtp() {
        assertThrows(BadRequestException.class, () -> service.validateOtp("admin@example.com", 7L, " "));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("OTP:admin@example.com", "otpHash")).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.validateOtp("admin@example.com", 7L, "123456"));
        assertThrows(BadRequestException.class, () -> service.storeOtp(null, 7L, "123456"));
    }

    @Test
    void redisFailureShouldUseInMemoryFallbackForStoreValidateAndDelete() {
        properties.getInfrastructure().setAllowInMemoryOtpFallback(true);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        service.storeOtp("admin@example.com", 7L, "123456");
        assertDoesNotThrow(() -> service.validateOtp("admin@example.com", 7L, "123456"));
        doThrow(new RuntimeException("redis down")).when(redisTemplate).delete("OTP:admin@example.com");
        assertDoesNotThrow(() -> service.deleteOtp("admin@example.com", 7L));
        assertThrows(BadRequestException.class,
                () -> service.validateOtp("admin@example.com", 7L, "123456"));
    }

    @Test
    void redisStoreFailureShouldPersistDbFallback() {
        Admin admin = new Admin();
        properties.getInfrastructure().setAllowInMemoryOtpFallback(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));
        when(adminRepository.findById(7L)).thenReturn(Optional.of(admin));

        service.storeOtp("admin@example.com", 7L, "123456");

        verify(adminOtpRepository).save(argThat(otp -> otp.getAdmin() == admin
                && otp.getOtpCode().equals("hash") && otp.getExpiresAt().isAfter(LocalDateTime.now())));
        assertThrows(InfrastructureException.class,
                () -> service.storeOtp("admin@example.com", null, "123456"));
    }

    @Test
    void redisValidationFailureShouldValidateAndConsumeDbOtp() {
        properties.getInfrastructure().setAllowInMemoryOtpFallback(false);
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));
        AdminOtp otp = otp("hash", 0, LocalDateTime.now().plusMinutes(2));
        when(adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        service.validateOtp("admin@example.com", 7L, "123456");

        assertNotNull(otp.getConsumedAt());
        verify(adminOtpRepository).save(otp);
    }

    @Test
    void dbFallbackShouldRejectExpiredAndIncrementInvalidAttempts() {
        properties.getInfrastructure().setAllowInMemoryOtpFallback(false);
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));
        AdminOtp expired = otp("hash", 0, LocalDateTime.now().minusSeconds(1));
        when(adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class,
                () -> service.validateOtp("admin@example.com", 7L, "123456"));

        AdminOtp invalid = otp("hash", 2, LocalDateTime.now().plusMinutes(2));
        when(adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(8L)).thenReturn(Optional.of(invalid));
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);
        assertEquals("Maximum OTP attempts exceeded", assertThrows(BadRequestException.class,
                () -> service.validateOtp("admin@example.com", 8L, "000000")).getMessage());
        assertEquals(3, invalid.getAttempts());
    }

    @Test
    void deleteOtpShouldConsumeLatestDbOtpWhenRedisFails() {
        properties.getInfrastructure().setAllowInMemoryOtpFallback(false);
        doThrow(new RuntimeException("redis down")).when(redisTemplate).delete("OTP:admin@example.com");
        AdminOtp otp = otp("hash", 0, LocalDateTime.now().plusMinutes(2));
        when(adminOtpRepository.findTopByAdmin_IdOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(otp));

        service.deleteOtp("admin@example.com", 7L);

        assertNotNull(otp.getConsumedAt());
        verify(adminOtpRepository).save(otp);
        assertDoesNotThrow(() -> service.deleteOtp("admin@example.com", null));
    }

    private static AdminOtp otp(String hash, int attempts, LocalDateTime expiresAt) {
        AdminOtp otp = new AdminOtp();
        otp.setOtpCode(hash);
        otp.setAttempts(attempts);
        otp.setExpiresAt(expiresAt);
        return otp;
    }
}
