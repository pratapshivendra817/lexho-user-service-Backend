package com.lexho.admin.entity;

import com.lexho.common.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void onCreateShouldNormalizeEmailAndDefaultRole() {
        Admin admin = new Admin();
        admin.setEmail("  ADMIN@Lexho.COM ");
        admin.setPassword("hash");

        admin.onCreate();

        assertEquals("admin@lexho.com", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertNotNull(admin.getCreatedAt());
        assertNotNull(admin.getUpdatedAt());
    }

    @Test
    void registerFailedAttemptShouldLockAfterThreshold() {
        Admin admin = new Admin();
        admin.setFailedAttempts(2);

        admin.registerFailedAttempt(3, 15);

        assertTrue(admin.isAccountLocked());
        assertNotNull(admin.getLockUntil());
    }

    @Test
    void unlockIfCooldownExpiredShouldResetFlags() {
        Admin admin = new Admin();
        admin.setAccountLocked(true);
        admin.setFailedAttempts(5);
        admin.setLockUntil(LocalDateTime.now().minusMinutes(1));

        boolean unlocked = admin.unlockIfCooldownExpired(LocalDateTime.now());

        assertTrue(unlocked);
        assertFalse(admin.isAccountLocked());
        assertEquals(0, admin.getFailedAttempts());
        assertNull(admin.getLockUntil());
    }
}
