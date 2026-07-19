package com.lexho.user_service.geo.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeoEventDlqReplayService {
    private final RabbitTemplate rabbitTemplate;
    private final String dlqName;
    private final String exchange;
    private final String routingKey;

    public GeoEventDlqReplayService(RabbitTemplate rabbitTemplate,
                                    @Value("${app.geo.events.rabbit.dlq:geo.events.dlq}") String dlqName,
                                    @Value("${app.geo.events.rabbit.exchange:geo.events.exchange}") String exchange,
                                    @Value("${app.geo.events.rabbit.routing-key:geo.events}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.dlqName = dlqName;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public int replay(int maxMessages) {
        int safeMax = Math.max(1, Math.min(1000, maxMessages));
        int replayed = 0;
        while (replayed < safeMax) {
            Object message = rabbitTemplate.receiveAndConvert(dlqName);
            if (message == null) {
                break;
            }
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            replayed++;
        }
        return replayed;
    }
}

