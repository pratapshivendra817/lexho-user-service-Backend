package com.lexho.user_service.dispatch.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerGeoIndexServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final GeoOperations<String, String> geoOperations = mock(GeoOperations.class);
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @Test
    void upsertShouldWriteActiveGeoIndexAndLastSeen() {
        PartnerGeoIndexService service = service(false);
        wireRedisOps();

        service.upsertPartnerLocation(10L, 28.61, 77.20);

        verify(geoOperations).add(eq(PartnerGeoIndexService.PARTNER_GEO_KEY), any(), eq("10"));
        verify(geoOperations).add(eq(PartnerGeoIndexService.PARTNER_GEO_ACTIVE_KEY), any(), eq("10"));
        verify(zSetOperations).remove(PartnerGeoIndexService.PARTNER_GEO_HEARTBEAT_KEY, "10");
        verify(valueOperations).set(eq("dispatch:partners:last-seen:10"), anyString());
    }

    @Test
    void upsertShouldIgnoreNullPartnerId() {
        PartnerGeoIndexService service = service(false);

        service.upsertPartnerLocation(null, 28.61, 77.20);

        verify(redisTemplate, never()).opsForGeo();
    }

    @Test
    void removeShouldDeletePartnerFromAllIndexes() {
        PartnerGeoIndexService service = service(false);
        wireRedisOps();

        service.removePartner(10L);

        verify(zSetOperations).remove(PartnerGeoIndexService.PARTNER_GEO_KEY, "10");
        verify(zSetOperations).remove(PartnerGeoIndexService.PARTNER_GEO_ACTIVE_KEY, "10");
        verify(zSetOperations).remove(PartnerGeoIndexService.PARTNER_GEO_HEARTBEAT_KEY, "10");
        verify(redisTemplate).delete("dispatch:partners:last-seen:10");
    }

    @Test
    void findNearbyShouldReturnEmptyWhenRedisReturnsNull() {
        PartnerGeoIndexService service = service(false);
        wireRedisOps();
        when(geoOperations.search(anyString(), any(), any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(null);

        assertTrue(service.findNearby(28.61, 77.20, 5.0, 10).isEmpty());
    }

    @Test
    void distanceKmShouldReturnDistanceValueAndEmptyOnRedisFailure() {
        PartnerGeoIndexService service = service(false);
        wireRedisOps();
        when(geoOperations.distance(PartnerGeoIndexService.PARTNER_GEO_KEY, "1", "2", Metrics.KILOMETERS))
                .thenReturn(new Distance(2.4, Metrics.KILOMETERS));

        Optional<Double> distance = service.distanceKm(1L, 2L);

        assertEquals(2.4, distance.orElseThrow());

        when(geoOperations.distance(PartnerGeoIndexService.PARTNER_GEO_KEY, "1", "3", Metrics.KILOMETERS))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertTrue(service.distanceKm(1L, 3L).isEmpty());
    }

    @Test
    void findLastSeenShouldParseValidEpochAndIgnoreInvalidValues() {
        PartnerGeoIndexService service = service(false);
        wireRedisOps();
        when(valueOperations.get("dispatch:partners:last-seen:10")).thenReturn("12345");
        when(valueOperations.get("dispatch:partners:last-seen:11")).thenReturn("not-a-number");

        assertEquals(12345L, service.findLastSeenEpochMs(10L));
        assertEquals(null, service.findLastSeenEpochMs(11L));
    }

    @Test
    void partitionedUpsertRemoveAndFallbackSearchShouldUseShardKeys() {
        PartnerGeoIndexService service = service(true);
        wireRedisOps();
        when(geoOperations.search(anyString(), any(), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(null)
                .thenReturn(new GeoResults<>(List.of(new GeoResult<>(
                        new RedisGeoCommands.GeoLocation<>("2", new Point(77.2, 28.6)),
                        new Distance(1.2, Metrics.KILOMETERS)
                ))));
        when(valueOperations.get("dispatch:partners:geo:member-shard:10")).thenReturn("ttnf");

        service.upsertPartnerLocation(10L, 28.61, 77.20, true);
        service.removePartner(10L);

        assertEquals(1, service.findNearby(28.61, 77.20, 5.0, 10).size());
        verify(geoOperations).add(eq(PartnerGeoIndexService.PARTNER_GEO_HEARTBEAT_KEY), any(), eq("10"));
        verify(geoOperations).add(eq("dispatch:partners:geo:shard:ttnf"), any(), eq("10"));
        verify(valueOperations).set(eq("dispatch:partners:geo:member-shard:10"), eq("ttnf"));
        verify(zSetOperations).remove("dispatch:partners:geo:shard:ttnf", "10");
        verify(redisTemplate).delete("dispatch:partners:geo:member-shard:10");
    }

    @Test
    void searchAndLastSeenShouldHandleConnectionFailureAndBlankValues() {
        PartnerGeoIndexService service = service(true);
        wireRedisOps();
        when(geoOperations.search(anyString(), any(), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        when(valueOperations.get("dispatch:partners:last-seen:12")).thenReturn("   ");
        when(valueOperations.get("dispatch:partners:last-seen:13")).thenThrow(new RedisConnectionFailureException("down"));

        assertTrue(service.findNearby(28.61, 77.20, 5.0, 10).isEmpty());
        assertEquals(null, service.findLastSeenEpochMs(12L));
        assertEquals(null, service.findLastSeenEpochMs(13L));
    }

    @Test
    void partitionedSearchShouldReturnShardResultsWithoutGlobalFallback() {
        PartnerGeoIndexService service = service(true);
        wireRedisOps();
        GeoResults<RedisGeoCommands.GeoLocation<String>> shardResults = new GeoResults<>(List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("9", new Point(77.0, 28.0)),
                        new Distance(0.3, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("10", new Point(77.1, 28.1)),
                        new Distance(0.8, Metrics.KILOMETERS))
        ));
        when(geoOperations.search(anyString(), any(), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(shardResults);

        var results = service.findNearby(28.61, 77.20, 5.0, 2);

        assertEquals(2, results.size());
        assertEquals(Set.of("9", "10"),
                Set.of(results.get(0).getContent().getName(), results.get(1).getContent().getName()));
    }

    private PartnerGeoIndexService service(boolean partitionEnabled) {
        return new PartnerGeoIndexService(redisTemplate, partitionEnabled, 4);
    }

    private void wireRedisOps() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
