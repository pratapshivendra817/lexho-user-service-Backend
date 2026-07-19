package com.lexho.user_service.admin;

import com.lexho.user_service.admin.service.RestrictedAreaOpsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestrictedAreaOpsServiceTest {

    @Test
    void addAndRemoveUseLocalStoreWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        when(redisTemplate.opsForSet()).thenThrow(new RuntimeException("Redis unavailable"));
        RestrictedAreaOpsService service = new RestrictedAreaOpsService(redisTemplate);

        Map<String, Object> added = service.add(28.6139d, 77.2090d, 6);

        assertEquals("in-memory-fallback", added.get("storage"));
        assertTrue(service.isRestricted(28.6139d, 77.2090d));

        Map<String, Object> removed = service.remove((String) added.get("geohash"));

        assertEquals("in-memory-fallback", removed.get("storage"));
        assertEquals(1L, removed.get("removed"));
        assertFalse(service.isRestricted(28.6139d, 77.2090d));
    }
}
