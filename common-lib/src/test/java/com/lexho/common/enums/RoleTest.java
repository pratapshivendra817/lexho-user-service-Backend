package com.lexho.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void shouldContainExpectedRoles() {
        assertEquals(Role.CUSTOMER, Role.valueOf("CUSTOMER"));
        assertEquals(Role.PARTNER, Role.valueOf("PARTNER"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
    }
}
