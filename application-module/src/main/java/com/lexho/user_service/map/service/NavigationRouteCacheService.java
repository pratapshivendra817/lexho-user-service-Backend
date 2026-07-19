package com.lexho.user_service.map.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NavigationRouteCacheService {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public NavigationRouteCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                       ObjectMapper objectMapper) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    public Optional<CachedRoute> get(String key) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Optional.empty();
        }
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, CachedRoute.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void put(String key, CachedRoute route, Duration ttl) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(route), ttl);
        } catch (JsonProcessingException | RuntimeException ignored) {
            // best effort cache only
        }
    }

    public record CachedRoute(
            Double distanceKm,
            Integer durationMinutes,
            String polyline,
            String provider,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            Boolean trafficAware,
            Integer alternativesConsidered,
            LocalDateTime computedAt
    ) {
    }
}
