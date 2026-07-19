package com.lexho.user_service.dispatch.service;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PartnerGeoIndexService {
    private static final Logger log = LoggerFactory.getLogger(PartnerGeoIndexService.class);
    public static final String PARTNER_GEO_KEY = "dispatch:partners:geo";
    public static final String PARTNER_GEO_ACTIVE_KEY = "dispatch:partners:geo:active";
    public static final String PARTNER_GEO_HEARTBEAT_KEY = "dispatch:partners:geo:heartbeat";
    private static final String PARTNER_GEO_SHARD_PREFIX = "dispatch:partners:geo:shard:";
    private static final String PARTNER_GEO_MEMBER_SHARD = "dispatch:partners:geo:member-shard:";
    private static final String PARTNER_LAST_SEEN_KEY_PREFIX = "dispatch:partners:last-seen:";
    private final StringRedisTemplate redisTemplate;
    private final boolean partitionEnabled;
    private final int shardGeohashPrecision;

    public PartnerGeoIndexService(StringRedisTemplate redisTemplate,
                                  @Value("${app.dispatch.geo.partition.enabled:false}") boolean partitionEnabled,
                                  @Value("${app.dispatch.geo.partition.precision:4}") int shardGeohashPrecision) {
        this.redisTemplate = redisTemplate;
        this.partitionEnabled = partitionEnabled;
        this.shardGeohashPrecision = Math.max(3, Math.min(6, shardGeohashPrecision));
    }

    public void upsertPartnerLocation(Long partnerId, double lat, double lng) {
        upsertPartnerLocation(partnerId, lat, lng, false);
    }

    public void upsertPartnerLocation(Long partnerId, double lat, double lng, boolean heartbeatOnly) {
        if (partnerId == null) {
            return;
        }
        try {
            String targetKey = heartbeatOnly ? PARTNER_GEO_HEARTBEAT_KEY : PARTNER_GEO_ACTIVE_KEY;
            String otherKey = heartbeatOnly ? PARTNER_GEO_ACTIVE_KEY : PARTNER_GEO_HEARTBEAT_KEY;
            redisTemplate.opsForGeo().add(PARTNER_GEO_KEY, new Point(lng, lat), member(partnerId));
            redisTemplate.opsForGeo().add(targetKey, new Point(lng, lat), member(partnerId));
            redisTemplate.opsForZSet().remove(otherKey, member(partnerId));
            redisTemplate.opsForValue().set(PARTNER_LAST_SEEN_KEY_PREFIX + partnerId, String.valueOf(System.currentTimeMillis()));
            if (partitionEnabled) {
                String shard = shard(lat, lng);
                redisTemplate.opsForGeo().add(PARTNER_GEO_SHARD_PREFIX + shard, new Point(lng, lat), member(partnerId));
                redisTemplate.opsForValue().set(PARTNER_GEO_MEMBER_SHARD + partnerId, shard);
            }
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while upserting partner geo index for partnerId={}", partnerId);
        }
    }

    public void removePartner(Long partnerId) {
        if (partnerId == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(PARTNER_GEO_KEY, member(partnerId));
            redisTemplate.opsForZSet().remove(PARTNER_GEO_ACTIVE_KEY, member(partnerId));
            redisTemplate.opsForZSet().remove(PARTNER_GEO_HEARTBEAT_KEY, member(partnerId));
            redisTemplate.delete(PARTNER_LAST_SEEN_KEY_PREFIX + partnerId);
            if (partitionEnabled) {
                String shard = redisTemplate.opsForValue().get(PARTNER_GEO_MEMBER_SHARD + partnerId);
                if (shard != null && !shard.isBlank()) {
                    redisTemplate.opsForZSet().remove(PARTNER_GEO_SHARD_PREFIX + shard, member(partnerId));
                }
                redisTemplate.delete(PARTNER_GEO_MEMBER_SHARD + partnerId);
            }
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while removing partner geo index for partnerId={}", partnerId);
        }
    }

    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearby(double lat, double lng, double radiusKm, long limit) {
        try {
            String key = PARTNER_GEO_KEY;
            if (partitionEnabled) {
                key = PARTNER_GEO_SHARD_PREFIX + shard(lat, lng);
            }
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                    key,
                    GeoReference.fromCoordinate(lng, lat),
                    new Distance(radiusKm, Metrics.KILOMETERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                            .includeDistance()
                            .sortAscending()
                            .limit(limit)
            );
            if (results == null) {
                if (partitionEnabled) {
                    return findNearbyFromGlobal(lat, lng, radiusKm, limit);
                }
                return List.of();
            }
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = new ArrayList<>(results.getContent());
            if (content.isEmpty() && partitionEnabled) {
                return findNearbyFromGlobal(lat, lng, radiusKm, limit);
            }
            return content;
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while finding nearby partners");
            return List.of();
        }
    }

    public Optional<Double> distanceKm(Long fromPartnerId, Long toPartnerId) {
        try {
            Distance distance = redisTemplate.opsForGeo().distance(PARTNER_GEO_KEY, member(fromPartnerId), member(toPartnerId), Metrics.KILOMETERS);
            return Optional.ofNullable(distance).map(Distance::getValue);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while calculating partner distance");
            return Optional.empty();
        }
    }

    private String member(Long partnerId) {
        return String.valueOf(partnerId);
    }

    private String shard(double lat, double lng) {
        return GeoHashUtil.encode(lat, lng, shardGeohashPrecision);
    }

    private List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearbyFromGlobal(double lat, double lng, double radiusKm, long limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> global = redisTemplate.opsForGeo().search(
                PARTNER_GEO_KEY,
                GeoReference.fromCoordinate(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit)
        );
        return global == null ? List.of() : new ArrayList<>(global.getContent());
    }

    public Long findLastSeenEpochMs(Long partnerId) {
        String raw;
        try {
            raw = redisTemplate.opsForValue().get(PARTNER_LAST_SEEN_KEY_PREFIX + partnerId);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while reading partner last-seen for partnerId={}", partnerId);
            return null;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
