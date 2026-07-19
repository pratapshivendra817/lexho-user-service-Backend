package com.lexho.user_service.geo.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GeoEventConsumers {
    private static final Logger log = LoggerFactory.getLogger(GeoEventConsumers.class);

    @RabbitListener(
            queues = "${app.geo.events.rabbit.queue:geo.events.queue}",
            containerFactory = "geoEventRabbitListenerContainerFactory",
            autoStartup = "${app.geo.events.rabbit.consumer.enabled:false}"
    )
    public void consumeRabbit(String payload) {
        process(payload, "rabbit");
    }

    @RabbitListener(
            queues = "${app.geo.events.rabbit.dlq:geo.events.dlq}",
            containerFactory = "geoEventRabbitListenerContainerFactory",
            autoStartup = "${app.geo.events.rabbit.dlq-consumer.enabled:false}"
    )
    public void consumeRabbitDlq(String payload) {
        log.warn("geo event DLQ message received: {}", payload);
    }

    private void process(String payload, String source) {
        // TODO: attach downstream processors (analytics, fraud, assignment, notifications)
        log.debug("geo event consumed from {}: {}", source, payload);
    }
}
