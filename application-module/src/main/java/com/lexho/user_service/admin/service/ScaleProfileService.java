package com.lexho.user_service.admin.service;

import com.lexho.user_service.util.LexhoConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ScaleProfileService {
    private static final Logger log = LoggerFactory.getLogger(ScaleProfileService.class);
    private static final String ACTIVE_PROFILE_KEY = "ops:scale:active-profile";
    private static final String CONFIG_PREFIX = "ops:scale:config:";
    private static final String STORAGE_IN_MEMORY_FALLBACK = "in-memory-fallback";
    private static final String STORAGE_REDIS = "redis";
    private static final String DISPATCH_GEO_PARTITION_ENABLED = "dispatch.geo.partition.enabled";
    private static final String GEO_EVENTS_RABBIT_PARTITION_ENABLED = "geo.events.rabbit.partition.enabled";
    private static final String GEO_EVENTS_RABBIT_PARTITION_COUNT = "geo.events.rabbit.partition.count";
    private static final String TRACKING_SSE_EN_ROUTE_MS = "tracking.sse.en_route.ms";
    private static final Set<String> ALLOWED = Set.of("1k", "10k", "100k");

    private final StringRedisTemplate redisTemplate;
    private volatile String localActiveProfile = "1k";

    public ScaleProfileService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> current() {
        String active = localActiveProfile;
        Map<String, String> config;
        String storage = STORAGE_REDIS;
        try {
            String redisActive = redisTemplate.opsForValue().get(ACTIVE_PROFILE_KEY);
            if (redisActive != null && !redisActive.isBlank()) {
                active = redisActive;
                localActiveProfile = redisActive;
            }
            config = redisTemplate.<String, String>opsForHash().entries(CONFIG_PREFIX + active);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while reading scale profile. Falling back to in-memory preset");
            config = preset(active);
            storage = STORAGE_IN_MEMORY_FALLBACK;
        }
        if (config == null || config.isEmpty()) {
            config = preset(active);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("activeProfile", active);
        response.put("config", config);
        response.put("allowedProfiles", ALLOWED);
        response.put(LexhoConstants.FIELD_STORAGE, storage);
        return response;
    }

    public Map<String, Object> applyProfile(String profile, boolean dryRun) {
        String normalized = normalize(profile);
        if (!ALLOWED.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported profile. Allowed: 1k, 10k, 100k");
        }
        Map<String, String> preset = preset(normalized);
        Map<String, Object> result = new HashMap<>();
        result.put("profile", normalized);
        result.put("config", preset);
        result.put("dryRun", dryRun);
        if (!dryRun) {
            localActiveProfile = normalized;
            try {
                redisTemplate.opsForValue().set(ACTIVE_PROFILE_KEY, normalized, Duration.ofDays(30));
                redisTemplate.delete(CONFIG_PREFIX + normalized);
                if (!preset.isEmpty()) {
                    redisTemplate.opsForHash().putAll(CONFIG_PREFIX + normalized, preset);
                }
                result.put(LexhoConstants.FIELD_STORAGE, STORAGE_REDIS);
            } catch (RuntimeException ex) {
                log.warn("Redis unavailable while applying scale profile. Falling back to in-memory preset");
                result.put(LexhoConstants.FIELD_STORAGE, STORAGE_IN_MEMORY_FALLBACK);
            }
            result.put("applied", true);
            result.put("note", "Applied in runtime control-plane. Restart may be needed for env-bound properties.");
        } else {
            result.put("applied", false);
        }
        return result;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, String> preset(String profile) {
        Map<String, String> map = new HashMap<>();
        switch (profile) {
            case "1k" -> {
                map.put(DISPATCH_GEO_PARTITION_ENABLED, "false");
                map.put(GEO_EVENTS_RABBIT_PARTITION_ENABLED, "false");
                map.put(GEO_EVENTS_RABBIT_PARTITION_COUNT, "1");
                map.put(TRACKING_SSE_EN_ROUTE_MS, "9000");
            }
            case "10k" -> {
                map.put(DISPATCH_GEO_PARTITION_ENABLED, "true");
                map.put("dispatch.geo.partition.precision", "4");
                map.put(GEO_EVENTS_RABBIT_PARTITION_ENABLED, "true");
                map.put(GEO_EVENTS_RABBIT_PARTITION_COUNT, "16");
                map.put(TRACKING_SSE_EN_ROUTE_MS, "7000");
            }
            case "100k" -> {
                map.put(DISPATCH_GEO_PARTITION_ENABLED, "true");
                map.put("dispatch.geo.partition.precision", "5");
                map.put(GEO_EVENTS_RABBIT_PARTITION_ENABLED, "true");
                map.put(GEO_EVENTS_RABBIT_PARTITION_COUNT, "64");
                map.put(TRACKING_SSE_EN_ROUTE_MS, "5000");
            }
            default -> {
            }
        }
        return map;
    }
}
