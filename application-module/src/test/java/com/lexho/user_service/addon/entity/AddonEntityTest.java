package com.lexho.user_service.addon.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AddonEntityTest {

    @Test
    void lifecycleCallbacksShouldSetTimestampsAndNormalizeQuantities() {
        AddonEntity addon = new AddonEntity();
        addon.setQuantityAllowed(false);
        addon.setMinQuantity(0);
        addon.setMaxQuantity(null);

        addon.onCreate();

        assertNotNull(addon.getCreatedAt());
        assertNotNull(addon.getUpdatedAt());
        assertEquals(1, addon.getMinQuantity());
        assertEquals(1, addon.getMaxQuantity());

        addon.setQuantityAllowed(true);
        addon.setMinQuantity(3);
        addon.setMaxQuantity(2);

        addon.onUpdate();

        assertEquals(3, addon.getMinQuantity());
        assertEquals(3, addon.getMaxQuantity());
        assertNotNull(addon.getUpdatedAt());
    }
}
