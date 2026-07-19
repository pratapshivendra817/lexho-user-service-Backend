package com.lexho.user_service.service;

import com.lexho.user_service.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalRecord> localStore = new ConcurrentHashMap<>();

    public ApiRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void check(String bucket, String keySuffix, int maxRequests, Duration window, String errorMessage) {
        String key = "rl:" + bucket + ":" + (keySuffix == null ? "anon" : keySuffix.trim());
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, window);
            }
            if (current != null && current > maxRequests) {
                throw new TooManyRequestsException(errorMessage, retryAfterSeconds(key, window));
            }
        } catch (RuntimeException ex) {
            if (ex instanceof TooManyRequestsException) {
                throw ex;
            }
            log.warn("Redis rate limit fallback for key={}", key);
            checkLocal(key, maxRequests, window, errorMessage);
        }
    }

    private void checkLocal(String key, int maxRequests, Duration window, String errorMessage) {
        long now = System.currentTimeMillis();
        LocalRecord record = localStore.compute(key, (k, current) -> {
            if (current == null || current.expiresAtMs <= now) {
                return new LocalRecord(1, now + window.toMillis());
            }
            current.count += 1;
            return current;
        });
        if (record != null && record.count > maxRequests) {
            long retryAfterSeconds = Math.max(1, (record.expiresAtMs - now + 999) / 1000);
            throw new TooManyRequestsException(errorMessage, retryAfterSeconds);
        }
    }

    private long retryAfterSeconds(String key, Duration defaultWindow) {
        try {
            Long ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return ttl;
            }
        } catch (RuntimeException ignored) {
            // Fall back to the configured window if Redis cannot report TTL.
        }
        return Math.max(1, defaultWindow.toSeconds());
    }

    private static class LocalRecord {
        private int count;
        private final long expiresAtMs;

        private LocalRecord(int count, long expiresAtMs) {
            this.count = count;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
