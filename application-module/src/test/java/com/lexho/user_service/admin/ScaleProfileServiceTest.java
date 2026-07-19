package com.lexho.user_service.admin;

import com.lexho.user_service.admin.service.ScaleProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScaleProfileServiceTest {

    @Test
    void currentFallsBackToPresetWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        when(redisTemplate.opsForValue().get("ops:scale:active-profile"))
                .thenThrow(new RuntimeException("Redis unavailable"));
        ScaleProfileService service = new ScaleProfileService(redisTemplate);

        Map<String, Object> response = service.current();

        assertEquals("1k", response.get("activeProfile"));
        assertEquals("in-memory-fallback", response.get("storage"));
    }

    @Test
    void applyProfileKeepsLocalProfileWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis unavailable"));
        ScaleProfileService service = new ScaleProfileService(redisTemplate);

        Map<String, Object> applied = service.applyProfile("10k", false);
        Map<String, Object> current = service.current();

        assertEquals("in-memory-fallback", applied.get("storage"));
        assertEquals("10k", current.get("activeProfile"));
    }
}
