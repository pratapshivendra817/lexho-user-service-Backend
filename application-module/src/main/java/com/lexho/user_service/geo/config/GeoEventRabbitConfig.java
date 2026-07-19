package com.lexho.user_service.geo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoEventRabbitConfig {

    @Bean
    public TopicExchange geoEventExchange(@Value("${app.geo.events.rabbit.exchange:geo.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue geoEventQueue(@Value("${app.geo.events.rabbit.queue:geo.events.queue}") String queueName,
                               @Value("${app.geo.events.rabbit.dlq:geo.events.dlq}") String dlqName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", dlqName)
                .build();
    }

    @Bean
    public Queue geoEventDlq(@Value("${app.geo.events.rabbit.dlq:geo.events.dlq}") String dlqName) {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding geoEventBinding(Queue geoEventQueue,
                                   TopicExchange geoEventExchange,
                                   @Value("${app.geo.events.rabbit.routing-key:geo.events}") String routingKey) {
        return BindingBuilder.bind(geoEventQueue).to(geoEventExchange).with(routingKey + ".#");
    }
}
