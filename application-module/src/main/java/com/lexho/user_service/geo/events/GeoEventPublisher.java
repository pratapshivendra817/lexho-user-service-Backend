package com.lexho.user_service.geo.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class GeoEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(GeoEventPublisher.class);
    private final ApplicationEventPublisher publisher;

    public GeoEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(GeoEventType type, Long actorId, Map<String, Object> payload) {
        try {
            publisher.publishEvent(new GeoEvent(type, actorId, Instant.now(), payload));
        } catch (Exception ex) {
            log.warn("geo event publish failed: type={} actorId={} error={}", type, actorId, ex.getMessage());
        }
    }
}

