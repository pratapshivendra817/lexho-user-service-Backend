package com.lexho.user_service.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationRouteCacheServiceTest {

    @Test
    void getAndPutUseRedisWhenAvailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        NavigationRouteCacheService service = new NavigationRouteCacheService(provider(redisTemplate), new ObjectMapper());
        NavigationRouteCacheService.CachedRoute route =
                new NavigationRouteCacheService.CachedRoute(4.2, 12, "polyline", "MAPBOX", 28.61, 77.20, 28.62, 77.21, true, 2, null);

        service.put("navigation:key", route, Duration.ofSeconds(30));
        when(valueOperations.get("navigation:key"))
                .thenReturn("{\"distanceKm\":4.2,\"durationMinutes\":12,\"polyline\":\"polyline\",\"provider\":\"MAPBOX\",\"originLatitude\":28.61,\"originLongitude\":77.2,\"destinationLatitude\":28.62,\"destinationLongitude\":77.21,\"trafficAware\":true,\"alternativesConsidered\":2}");

        Optional<NavigationRouteCacheService.CachedRoute> cached = service.get("navigation:key");

        assertThat(cached).isPresent();
        assertThat(cached.get().durationMinutes()).isEqualTo(12);
        verify(valueOperations).set("navigation:key", "{\"distanceKm\":4.2,\"durationMinutes\":12,\"polyline\":\"polyline\",\"provider\":\"MAPBOX\",\"originLatitude\":28.61,\"originLongitude\":77.2,\"destinationLatitude\":28.62,\"destinationLongitude\":77.21,\"trafficAware\":true,\"alternativesConsidered\":2,\"computedAt\":null}", Duration.ofSeconds(30));
    }

    @Test
    void missingRedisProviderReturnsEmptyAndDoesNotFail() {
        NavigationRouteCacheService service = new NavigationRouteCacheService(provider(null), new ObjectMapper());

        assertThat(service.get("missing")).isEmpty();
        service.put("missing", new NavigationRouteCacheService.CachedRoute(1.0, 1, null, "X", null, null, null, null, false, 1, null), Duration.ofSeconds(1));
    }

    private ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }
        };
    }
}
