package com.lexho.user_service.service;

import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.InfrastructureException;
import com.lexho.user_service.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminLoginRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(AdminLoginRateLimiterService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final AdminAuthProperties adminAuthProperties;
    private final Map<String, LocalRateLimitRecord> localRateLimitStore = new ConcurrentHashMap<>();

    public AdminLoginRateLimiterService(StringRedisTemplate stringRedisTemplate,
                                        AdminAuthProperties adminAuthProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.adminAuthProperties = adminAuthProperties;
    }

    public void validateAllowed(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String key = adminAuthProperties.getRateLimit().getKeyPrefix() + normalizedEmail;
        Duration window = Duration.ofSeconds(adminAuthProperties.getRateLimit().getWindowSeconds());
        int maxRequests = adminAuthProperties.getRateLimit().getMaxRequests();

        try {
            Long current = stringRedisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                stringRedisTemplate.expire(key, window);
            }

            if (current != null && current > maxRequests) {
                throw new TooManyRequestsException("Too many login attempts. Please try again later");
            }
        } catch (RuntimeException ex) {
            if (ex instanceof TooManyRequestsException) {
                throw ex;
            }
            if (!adminAuthProperties.getInfrastructure().isAllowInMemoryRateLimitFallback()) {
                throw new InfrastructureException("Login service is temporarily unavailable");
            }
            log.warn("Redis unavailable while checking admin login rate limit. Falling back to in-memory store");
            validateLocalRateLimit(key, window, maxRequests);
        }
    }

    private void validateLocalRateLimit(String key, Duration window, int maxRequests) {
        long now = System.currentTimeMillis();
        LocalRateLimitRecord updated = localRateLimitStore.compute(key, (k, current) -> {
            if (current == null || current.expiresAtEpochMs <= now) {
                return new LocalRateLimitRecord(1, now + window.toMillis());
            }
            current.count += 1;
            return current;
        });

        if (updated != null && updated.count > maxRequests) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later");
        }
    }

    private static class LocalRateLimitRecord {
        private int count;
        private final long expiresAtEpochMs;

        private LocalRateLimitRecord(int count, long expiresAtEpochMs) {
            this.count = count;
            this.expiresAtEpochMs = expiresAtEpochMs;
        }
    }
}
