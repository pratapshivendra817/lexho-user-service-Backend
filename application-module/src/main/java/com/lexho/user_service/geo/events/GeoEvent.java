package com.lexho.user_service.geo.events;

import java.time.Instant;
import java.util.Map;

public record GeoEvent(
        GeoEventType type,
        Long actorId,
        Instant timestamp,
        Map<String, Object> payload
) {
}

