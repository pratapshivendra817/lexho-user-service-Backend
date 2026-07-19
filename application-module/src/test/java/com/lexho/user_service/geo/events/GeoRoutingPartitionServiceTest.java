package com.lexho.user_service.geo.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeoRoutingPartitionServiceTest {

    @Test
    void returnsBaseKeyWhenDisabledAndPartitionsByCoordinatesOrActor() {
        GeoEvent coordinates = new GeoEvent(GeoEventType.PARTNER_MOVED, 9L, Instant.now(),
                Map.of("lat", 28.61, "lng", 77.20));
        GeoEvent actorOnly = new GeoEvent(GeoEventType.PARTNER_ONLINE, 9L, Instant.now(), Map.of());
        GeoEvent anonymous = new GeoEvent(GeoEventType.PARTNER_OFFLINE, null, Instant.now(), null);

        assertThat(new GeoRoutingPartitionService(false, 8).routingKey("geo.events", coordinates))
                .isEqualTo("geo.events");
        assertThat(new GeoRoutingPartitionService(true, 8).routingKey("geo.events", coordinates))
                .matches("geo\\.events\\.p[0-7]");
        assertThat(new GeoRoutingPartitionService(true, 8).routingKey("geo.events", actorOnly))
                .isEqualTo("geo.events.p1");
        assertThat(new GeoRoutingPartitionService(true, 8).routingKey("geo.events", anonymous))
                .isEqualTo("geo.events.p0");
    }
}
