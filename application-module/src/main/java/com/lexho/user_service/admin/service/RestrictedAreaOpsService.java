package com.lexho.user_service.admin.service;

import com.lexho.user_service.dispatch.service.GeoHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RestrictedAreaOpsService {
    private static final Logger log = LoggerFactory.getLogger(RestrictedAreaOpsService.class);
    private static final String RESTRICTED_GEOHASH_SET = "location:serviceability:restricted:geohash";

    private final StringRedisTemplate redisTemplate;
    private final Set<String> localRestrictedAreas = ConcurrentHashMap.newKeySet();

    public RestrictedAreaOpsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> add(double lat, double lng, int precision) {
        String geohash = GeoHashUtil.encode(lat, lng, precision);
        String storage = "redis";
        try {
            redisTemplate.opsForSet().add(RESTRICTED_GEOHASH_SET, geohash);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while adding restricted area. Falling back to in-memory store");
            localRestrictedAreas.add(geohash);
            storage = "in-memory-fallback";
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("geohash", geohash);
        payload.put("restricted", true);
        payload.put("storage", storage);
        return payload;
    }

    public Map<String, Object> remove(String geohash) {
        long removed;
        String storage = "redis";
        try {
            Long redisRemoved = redisTemplate.opsForSet().remove(RESTRICTED_GEOHASH_SET, geohash);
            removed = redisRemoved == null ? 0L : redisRemoved;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while removing restricted area. Falling back to in-memory store");
            removed = localRestrictedAreas.remove(geohash) ? 1L : 0L;
            storage = "in-memory-fallback";
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("geohash", geohash);
        payload.put("removed", removed);
        payload.put("storage", storage);
        return payload;
    }

    public boolean isRestricted(double lat, double lng) {
        String geohash = GeoHashUtil.encode(lat, lng, 6);
        try {
            Boolean restricted = redisTemplate.opsForSet().isMember(RESTRICTED_GEOHASH_SET, geohash);
            return Boolean.TRUE.equals(restricted) || localRestrictedAreas.contains(geohash);
        } catch (RuntimeException ex) {
            return localRestrictedAreas.contains(geohash);
        }
    }
}
