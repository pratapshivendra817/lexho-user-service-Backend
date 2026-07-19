package com.lexho.user_service.geo.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeoEventMessageBridgeTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final GeoRoutingPartitionService routingPartitionService = mock(GeoRoutingPartitionService.class);
    private final GeoEvent event = new GeoEvent(GeoEventType.LOCATION_UPDATED, 7L, Instant.parse("2026-06-20T12:00:00Z"),
            Map.of("lat", 28.61, "lng", 77.20));

    @Test
    void shouldPublishWhenRabbitIsEnabled() throws Exception {
        GeoEventMessageBridge bridge = new GeoEventMessageBridge(
                rabbitTemplate, objectMapper, routingPartitionService, true, "geo.exchange", "geo.events");
        when(objectMapper.writeValueAsString(event)).thenReturn("{json}");
        when(routingPartitionService.routingKey("geo.events", event)).thenReturn("geo.events.p1");

        bridge.onGeoEvent(event);

        verify(rabbitTemplate).convertAndSend("geo.exchange", "geo.events.p1", (Object) "{json}");
    }

    @Test
    void shouldSkipRabbitWhenDisabledButStillSerialize() throws Exception {
        GeoEventMessageBridge bridge = new GeoEventMessageBridge(
                rabbitTemplate, objectMapper, routingPartitionService, false, "geo.exchange", "geo.events");
        when(objectMapper.writeValueAsString(event)).thenReturn("{json}");

        bridge.onGeoEvent(event);

        verify(objectMapper).writeValueAsString(event);
        verify(rabbitTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void shouldSwallowSerializationAndPublishFailures() throws Exception {
        GeoEventMessageBridge serializationBridge = new GeoEventMessageBridge(
                rabbitTemplate, objectMapper, routingPartitionService, true, "geo.exchange", "geo.events");
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("bad") { });

        serializationBridge.onGeoEvent(event);

        ObjectMapper publishMapper = mock(ObjectMapper.class);
        GeoEventMessageBridge publishBridge = new GeoEventMessageBridge(
                rabbitTemplate, publishMapper, routingPartitionService, true, "geo.exchange", "geo.events");
        when(publishMapper.writeValueAsString(event)).thenReturn("{json}");
        when(routingPartitionService.routingKey("geo.events", event)).thenReturn("geo.events.p1");
        doThrow(new RuntimeException("down")).when(rabbitTemplate).convertAndSend("geo.exchange", "geo.events.p1", (Object) "{json}");

        publishBridge.onGeoEvent(event);
    }
}
