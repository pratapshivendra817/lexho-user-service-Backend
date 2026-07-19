package com.lexho.user_service.map.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NavigationEtaTrackingRegistry {

    private static final String LIVE_ETA_BOOKINGS_KEY = "navigation:live:bookings";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<Long, Long> localExpiries = new ConcurrentHashMap<>();

    public NavigationEtaTrackingRegistry(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public void register(Long bookingId, Duration ttl) {
        if (bookingId == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        long expiresAtEpochMs = System.currentTimeMillis() + ttl.toMillis();
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForZSet().add(LIVE_ETA_BOOKINGS_KEY, String.valueOf(bookingId), expiresAtEpochMs);
                return;
            } catch (RuntimeException ignored) {
                // Fall back to local registry.
            }
        }
        localExpiries.put(bookingId, expiresAtEpochMs);
    }

    public List<Long> activeBookingIds(int limit) {
        long now = System.currentTimeMillis();
        int safeLimit = Math.max(1, limit);
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForZSet().removeRangeByScore(LIVE_ETA_BOOKINGS_KEY, 0, now - 1);
                Set<String> rawIds = redisTemplate.opsForZSet()
                        .rangeByScore(LIVE_ETA_BOOKINGS_KEY, now, Double.MAX_VALUE, 0, safeLimit);
                if (rawIds == null || rawIds.isEmpty()) {
                    return List.of();
                }
                return rawIds.stream()
                        .map(this::parseBookingId)
                        .filter(id -> id != null)
                        .toList();
            } catch (RuntimeException ignored) {
                // Fall back to local registry.
            }
        }

        localExpiries.entrySet().removeIf(entry -> entry.getValue() < now);
        return localExpiries.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .limit(safeLimit)
                .map(Map.Entry::getKey)
                .toList();
    }

    public void unregister(Long bookingId) {
        if (bookingId == null) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForZSet().remove(LIVE_ETA_BOOKINGS_KEY, String.valueOf(bookingId));
            } catch (RuntimeException ignored) {
                // Keep local cleanup below.
            }
        }
        localExpiries.remove(bookingId);
    }

    private Long parseBookingId(String rawId) {
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
