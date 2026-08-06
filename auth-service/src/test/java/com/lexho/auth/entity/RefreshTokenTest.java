package com.lexho.auth.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RefreshTokenTest {

    @Test
    void shouldSupportLifecycleAccessorsAndObjectMethods() {
        RefreshToken first = new RefreshToken();
        ReflectionTestUtils.setField(first, "id", 1L);
        first.setUserId(10L);
        first.setToken("token");
        first.setRole("ADMIN");
        first.setExpiresAt(LocalDateTime.of(2026, 6, 20, 12, 0));
        first.setRevoked(false);
        first.onCreate();

        RefreshToken second = new RefreshToken();
        ReflectionTestUtils.setField(second, "id", 1L);
        second.setUserId(10L);
        second.setToken("token");
        second.setRole("ADMIN");
        second.setExpiresAt(first.getExpiresAt());
        second.setRevoked(false);
        ReflectionTestUtils.setField(second, "createdAt", first.getCreatedAt());

        assertEquals(1L, first.getId());
        assertEquals(10L, first.getUserId());
        assertEquals("token", first.getToken());
        assertEquals("ADMIN", first.getRole());
        assertEquals(LocalDateTime.of(2026, 6, 20, 12, 0), first.getExpiresAt());
        assertEquals(false, first.isRevoked());
        assertNotNull(first.getCreatedAt());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("RefreshToken{id=1, userId=10, token='[REDACTED]', role='ADMIN', expiresAt="
                + first.getExpiresAt() + ", revoked=false, createdAt=" + first.getCreatedAt() + "}", first.toString());

        second.setRevoked(true);
        assertNotEquals(first, second);
    }
}
