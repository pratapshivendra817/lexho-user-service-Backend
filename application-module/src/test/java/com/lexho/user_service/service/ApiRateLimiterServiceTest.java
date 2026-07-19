package com.lexho.user_service.service;

import com.lexho.user_service.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimiterServiceTest {

    @Test
    void checkShouldIncrementRedisKeyAndExpireFirstRequest() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.increment("rl:otp:9999999999")).thenReturn(1L);
        ApiRateLimiterService service = new ApiRateLimiterService(redisTemplate);

        service.check("otp", " 9999999999 ", 5, Duration.ofMinutes(15), "Too many requests");

        verify(redisTemplate).expire("rl:otp:9999999999", Duration.ofMinutes(15));
    }

    @Test
    void checkShouldThrowTooManyRequestsWithRedisTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.increment("rl:payment:booking-1")).thenReturn(6L);
        when(redisTemplate.getExpire("rl:payment:booking-1", TimeUnit.SECONDS)).thenReturn(42L);
        ApiRateLimiterService service = new ApiRateLimiterService(redisTemplate);

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> service.check("payment", "booking-1", 5, Duration.ofMinutes(1), "Too many requests"));

        assertEquals("Too many requests", exception.getMessage());
        assertEquals(42L, exception.getRetryAfterSeconds());
    }

    @Test
    void checkShouldUseLocalFallbackWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        ApiRateLimiterService service = new ApiRateLimiterService(redisTemplate);

        service.check("login", "admin@example.com", 1, Duration.ofSeconds(30), "Too many requests");

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> service.check("login", "admin@example.com", 1, Duration.ofSeconds(30), "Too many requests"));

        assertEquals(30L, exception.getRetryAfterSeconds());
    }
}
