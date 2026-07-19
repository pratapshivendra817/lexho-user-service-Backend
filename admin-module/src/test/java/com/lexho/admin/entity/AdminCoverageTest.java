package com.lexho.admin.entity;

import com.lexho.common.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCoverageTest {

    @Test
    void defaultConstructorStartsWithNullsAndPrimitiveDefaults() {
        Admin admin = new Admin();

        assertThat(admin.getId()).isNull();
        assertThat(admin.getEmail()).isNull();
        assertThat(admin.getPassword()).isNull();
        assertThat(admin.getPasswordHash()).isNull();
        assertThat(admin.getRole()).isNull();
        assertThat(admin.isActive()).isFalse();
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.isAccountLocked()).isFalse();
        assertThat(admin.getLockUntil()).isNull();
        assertThat(admin.getCreatedAt()).isNull();
        assertThat(admin.getUpdatedAt()).isNull();
    }

    @Test
    void gettersAndSettersCoverAllFieldsIncludingPasswordHashAlias() {
        Admin admin = new Admin();
        LocalDateTime lockUntil = LocalDateTime.of(2026, 6, 20, 10, 15);

        admin.setId(7L);
        admin.setEmail("ADMIN@LEXHO.COM");
        admin.setPassword("secret");
        admin.setPasswordHash("hash");
        admin.setRole(Role.PARTNER);
        admin.setActive(true);
        admin.setFailedAttempts(3);
        admin.setAccountLocked(true);
        admin.setLockUntil(lockUntil);

        assertThat(admin.getId()).isEqualTo(7L);
        assertThat(admin.getEmail()).isEqualTo("ADMIN@LEXHO.COM");
        assertThat(admin.getPassword()).isEqualTo("hash");
        assertThat(admin.getPasswordHash()).isEqualTo("hash");
        assertThat(admin.getRole()).isEqualTo(Role.PARTNER);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getFailedAttempts()).isEqualTo(3);
        assertThat(admin.isAccountLocked()).isTrue();
        assertThat(admin.getLockUntil()).isEqualTo(lockUntil);
    }

    @Test
    void onCreateNormalizesEmailSetsTimestampsAndDefaultsRole() {
        Admin admin = new Admin();
        admin.setEmail("  ADMIN@Lexho.COM ");
        admin.setPassword("hash");

        admin.onCreate();

        assertThat(admin.getEmail()).isEqualTo("admin@lexho.com");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isEqualTo(admin.getCreatedAt());
    }

    @Test
    void onCreatePreservesExplicitRoleAndAllowsNullEmail() {
        Admin admin = new Admin();
        admin.setRole(Role.PARTNER);

        admin.onCreate();

        assertThat(admin.getRole()).isEqualTo(Role.PARTNER);
        assertThat(admin.getEmail()).isNull();
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesTimestampAndNormalizesEmail() {
        Admin admin = new Admin();
        admin.setEmail("  Partner@Lexho.com ");
        admin.onCreate();
        LocalDateTime createdAt = admin.getCreatedAt();

        admin.setEmail("  UPDATED@LEXHO.COM ");
        admin.onUpdate();

        assertThat(admin.getEmail()).isEqualTo("updated@lexho.com");
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(admin.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void registerFailedAttemptIncrementsWithoutLockingBeforeThreshold() {
        Admin admin = new Admin();
        admin.setFailedAttempts(1);

        admin.registerFailedAttempt(3, 15);

        assertThat(admin.getFailedAttempts()).isEqualTo(2);
        assertThat(admin.isAccountLocked()).isFalse();
        assertThat(admin.getLockUntil()).isNull();
    }

    @Test
    void registerFailedAttemptLocksAndSetsCooldownWhenThresholdReached() {
        Admin admin = new Admin();
        admin.setFailedAttempts(2);

        admin.registerFailedAttempt(3, 15);

        assertThat(admin.getFailedAttempts()).isEqualTo(3);
        assertThat(admin.isAccountLocked()).isTrue();
        assertThat(admin.getLockUntil()).isNotNull();
    }

    @Test
    void registerFailedAttemptLocksWithoutCooldownWhenDurationIsNonPositive() {
        Admin admin = new Admin();

        admin.registerFailedAttempt(1, 0);

        assertThat(admin.getFailedAttempts()).isEqualTo(1);
        assertThat(admin.isAccountLocked()).isTrue();
        assertThat(admin.getLockUntil()).isNull();
    }

    @Test
    void resetFailedAttemptsClearsCountersAndLockState() {
        Admin admin = new Admin();
        admin.setFailedAttempts(5);
        admin.setAccountLocked(true);
        admin.setLockUntil(LocalDateTime.now().plusMinutes(5));

        admin.resetFailedAttempts();

        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.isAccountLocked()).isFalse();
        assertThat(admin.getLockUntil()).isNull();
    }

    @Test
    void unlockIfCooldownExpiredReturnsFalseWhenNotLockedOrCooldownMissingOrNotExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 20, 12, 0);

        Admin unlockedAdmin = new Admin();
        assertThat(unlockedAdmin.unlockIfCooldownExpired(now)).isFalse();

        Admin noCooldownAdmin = new Admin();
        noCooldownAdmin.setAccountLocked(true);
        assertThat(noCooldownAdmin.unlockIfCooldownExpired(now)).isFalse();

        Admin futureCooldownAdmin = new Admin();
        futureCooldownAdmin.setAccountLocked(true);
        futureCooldownAdmin.setFailedAttempts(4);
        futureCooldownAdmin.setLockUntil(now.plusMinutes(1));
        assertThat(futureCooldownAdmin.unlockIfCooldownExpired(now)).isFalse();
        assertThat(futureCooldownAdmin.isAccountLocked()).isTrue();
        assertThat(futureCooldownAdmin.getFailedAttempts()).isEqualTo(4);
    }

    @Test
    void unlockIfCooldownExpiredResetsStateWhenExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 20, 12, 0);
        Admin admin = new Admin();
        admin.setAccountLocked(true);
        admin.setFailedAttempts(5);
        admin.setLockUntil(now.minusSeconds(1));

        boolean unlocked = admin.unlockIfCooldownExpired(now);

        assertThat(unlocked).isTrue();
        assertThat(admin.isAccountLocked()).isFalse();
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockUntil()).isNull();
    }

    @Test
    void objectMethodsFollowPlainJavaIdentitySemantics() {
        Admin first = new Admin();
        Admin second = new Admin();

        assertThat(first).isEqualTo(first);
        assertThat(first).isNotEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(System.identityHashCode(first));
        assertThat(first.toString()).contains("com.lexho.admin.entity.Admin");
    }
}
