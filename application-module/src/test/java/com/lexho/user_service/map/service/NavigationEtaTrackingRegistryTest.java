package com.lexho.user_service.map.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationEtaTrackingRegistryTest {

    @Test
    void localRegistryTracksAndExpiresBookingsWithoutRedis() throws Exception {
        NavigationEtaTrackingRegistry registry = new NavigationEtaTrackingRegistry(provider(null));

        registry.register(101L, Duration.ofMillis(50));

        assertThat(registry.activeBookingIds(10)).containsExactly(101L);
        Thread.sleep(70L);
        assertThat(registry.activeBookingIds(10)).isEmpty();
    }

    @Test
    void unregisterRemovesBookingFromLocalRegistry() {
        NavigationEtaTrackingRegistry registry = new NavigationEtaTrackingRegistry(provider(null));
        registry.register(202L, Duration.ofSeconds(5));

        registry.unregister(202L);

        assertThat(registry.activeBookingIds(10)).isEmpty();
    }

    @Test
    void registerAndFetchShouldUseRedisWhenAvailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScore(org.mockito.ArgumentMatchers.eq("navigation:live:bookings"),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(new LinkedHashSet<>(List.of("501", "bad-id")));
        NavigationEtaTrackingRegistry registry = new NavigationEtaTrackingRegistry(provider(redisTemplate));

        registry.register(501L, Duration.ofSeconds(5));

        assertThat(registry.activeBookingIds(0)).containsExactly(501L);
        verify(zSetOperations).add(org.mockito.ArgumentMatchers.eq("navigation:live:bookings"),
                org.mockito.ArgumentMatchers.eq("501"), org.mockito.ArgumentMatchers.anyDouble());
        verify(zSetOperations).removeRangeByScore(org.mockito.ArgumentMatchers.eq("navigation:live:bookings"),
                org.mockito.ArgumentMatchers.eq(0.0), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void redisFailuresShouldFallbackToLocalRegistryAndIgnoreInvalidTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.add(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble())).thenThrow(new RuntimeException("redis down"));
        when(zSetOperations.removeRangeByScore(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble())).thenThrow(new RuntimeException("redis down"));
        NavigationEtaTrackingRegistry registry = new NavigationEtaTrackingRegistry(provider(redisTemplate));

        registry.register(null, Duration.ofSeconds(1));
        registry.register(700L, null);
        registry.register(701L, Duration.ZERO);
        registry.register(702L, Duration.ofSeconds(-1));
        registry.register(703L, Duration.ofSeconds(5));

        assertThat(registry.activeBookingIds(5)).containsExactly(703L);
    }

    @Test
    void unregisterShouldCleanRedisAndLocalState() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        NavigationEtaTrackingRegistry registry = new NavigationEtaTrackingRegistry(provider(redisTemplate));
        registry.register(900L, Duration.ofSeconds(5));

        registry.unregister(900L);

        verify(zSetOperations).remove("navigation:live:bookings", "900");
        assertThat(registry.activeBookingIds(10)).isEmpty();
    }

    private ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> provider(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
                return redisTemplate;
            }

            @Override
            public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }

            @Override
            public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
                return redisTemplate;
            }

            @Override
            public org.springframework.data.redis.core.StringRedisTemplate getObject() {
                return redisTemplate;
            }
        };
    }
}
