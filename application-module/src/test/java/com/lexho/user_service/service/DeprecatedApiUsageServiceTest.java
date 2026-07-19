package com.lexho.user_service.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeprecatedApiUsageServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void recordShouldIncrementCounterAndExposeLastSeenSnapshot() {
        DeprecatedApiUsageService service = new DeprecatedApiUsageService();

        service.record("/legacy/orders");
        service.record("/legacy/orders");

        Map<String, Object> snapshot = service.snapshot();
        Map<String, Object> item = (Map<String, Object>) snapshot.get("/legacy/orders");

        assertEquals(2L, item.get("count"));
        assertNotNull(item.get("lastSeen"));
    }
}
