package com.lexho.user.entity;

import com.lexho.common.enums.Role;
import com.lexho.user.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void otpConstructorShouldInitializeCustomerDefaultsWithoutCredentialLiteral() {
        User user = new User("9876543210", true);

        assertEquals("9876543210", user.getPhoneNumber());
        assertEquals("Guest User", user.getFullName());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0L, user.getTotalBookings());
        assertTrue(user.isActive());
        assertFalse(user.isDeleted());
        assertNotNull(user.getPassword());
    }

    @Test
    void lifecycleCallbacksShouldPopulateAuditFieldsAndStatusDefaults() {
        User user = new User();
        user.setPhoneNumber("9876543210");
        user.setFullName("Guest User");

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0L, user.getTotalBookings());
        assertTrue(user.isActive());

        user.setStatus(UserStatus.INACTIVE);
        user.onUpdate();

        assertFalse(user.isActive());
        assertNotNull(user.getUpdatedAt());
    }
}
