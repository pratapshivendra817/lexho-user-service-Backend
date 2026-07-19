package com.lexho.user_service.service;

import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.InfrastructureException;
import com.lexho.user_service.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminLoginRateLimiterServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final AdminAuthProperties properties = new AdminAuthProperties();

    @Test
    void incrementsRedisExpiresFirstAttemptAndRejectsBeyondLimit() {
        properties.getRateLimit().setKeyPrefix("login:");
        properties.getRateLimit().setWindowSeconds(60);
        properties.getRateLimit().setMaxRequests(2);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("login:admin@example.com")).thenReturn(1L, 3L);
        when(redis.getExpire("login:admin@example.com", TimeUnit.SECONDS)).thenReturn(17L);
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(redis, properties);

        service.validateAllowed(" Admin@Example.com ");
        verify(redis).expire("login:admin@example.com", Duration.ofSeconds(60));
        assertThatThrownBy(() -> service.validateAllowed("admin@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void usesLocalFallbackOrFailsClosedWhenRedisIsUnavailable() {
        properties.getRateLimit().setMaxRequests(1);
        properties.getInfrastructure().setAllowInMemoryRateLimitFallback(true);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(redis, properties);

        service.validateAllowed(null);
        assertThatThrownBy(() -> service.validateAllowed(null)).isInstanceOf(TooManyRequestsException.class);

        properties.getInfrastructure().setAllowInMemoryRateLimitFallback(false);
        assertThatThrownBy(() -> new AdminLoginRateLimiterService(redis, properties).validateAllowed("x"))
                .isInstanceOf(InfrastructureException.class);
    }
}
