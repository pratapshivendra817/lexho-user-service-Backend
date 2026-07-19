package com.lexho.user.service;

import com.lexho.user.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public RateLimiterService(Optional<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate.orElse(null);
    }

    public void check(String bucket, String identity, int maxRequests, Duration window, String message) {
        String key = "rate-limit:" + bucket + ":" + identity;
        if (checkRedisLimit(key, maxRequests, window, message)) {
            return;
        }
        checkLocalLimit(key, maxRequests, window, message);
    }

    private boolean checkRedisLimit(String key, int maxRequests, Duration window, String message) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Long counter = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(counter)) {
                redisTemplate.expire(key, window);
            }
            if (counter != null && counter > maxRequests) {
                throw new TooManyRequestsException(message, resolveRetryAfter(key, window));
            }
            return true;
        } catch (RuntimeException exception) {
            LOG.warn("Redis rate limiting unavailable for {}", key, exception);
            return false;
        }
    }

    private long resolveRetryAfter(String key, Duration window) {
        return Optional.ofNullable(redisTemplate.getExpire(key, TimeUnit.SECONDS))
                .filter(value -> value > 0)
                .orElse(Math.max(1L, window.toSeconds()));
    }

    private void checkLocalLimit(String key, int maxRequests, Duration window, String message) {
        long now = System.currentTimeMillis();
        LocalCounter counter = localCounters.compute(key, (currentKey, currentValue) -> {
            if (currentValue == null || currentValue.expiresAtMs <= now) {
                return new LocalCounter(1, now + window.toMillis());
            }
            currentValue.count++;
            return currentValue;
        });

        if (counter != null && counter.count > maxRequests) {
            long retryAfter = Math.max(1L, (counter.expiresAtMs - now + 999) / 1000);
            throw new TooManyRequestsException(message, retryAfter);
        }
    }

    private static final class LocalCounter {
        private int count;
        private final long expiresAtMs;

        private LocalCounter(int count, long expiresAtMs) {
            this.count = count;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
