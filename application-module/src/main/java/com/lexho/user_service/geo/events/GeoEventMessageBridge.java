package com.lexho.user_service.geo.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GeoEventMessageBridge {
    private static final Logger log = LoggerFactory.getLogger(GeoEventMessageBridge.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final boolean rabbitEnabled;
    private final String rabbitExchange;
    private final String rabbitRoutingKey;
    private final GeoRoutingPartitionService routingPartitionService;

    public GeoEventMessageBridge(RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper,
                                 GeoRoutingPartitionService routingPartitionService,
                                 @Value("${app.geo.events.rabbit.enabled:false}") boolean rabbitEnabled,
                                 @Value("${app.geo.events.rabbit.exchange:geo.events.exchange}") String rabbitExchange,
                                 @Value("${app.geo.events.rabbit.routing-key:geo.events}") String rabbitRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.routingPartitionService = routingPartitionService;
        this.rabbitEnabled = rabbitEnabled;
        this.rabbitExchange = rabbitExchange;
        this.rabbitRoutingKey = rabbitRoutingKey;
    }

    @EventListener
    public void onGeoEvent(GeoEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            if (rabbitEnabled) {
                String routingKey = routingPartitionService.routingKey(rabbitRoutingKey, event);
                rabbitTemplate.convertAndSend(rabbitExchange, routingKey, payload);
            }
        } catch (Exception ex) {
            log.warn("geo event bridge failed: type={} error={}", event.type(), ex.getMessage());
        }
    }
}
