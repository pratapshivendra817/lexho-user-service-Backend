package com.lexho.user_service.addon.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CartItemAddonEntityTest {

    @Test
    void lifecycleCallbacksShouldSetTimestamps() {
        CartItemAddonEntity entity = new CartItemAddonEntity();

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());

        entity.onUpdate();

        assertNotNull(entity.getUpdatedAt());
    }
}
