package com.lexho.user_service.geo.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoEventRabbitListenerConfig {

    @Bean(name = "geoEventRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory geoEventRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.geo.events.rabbit.retry-attempts:3}") int maxAttempts,
            @Value("${app.geo.events.rabbit.retry-initial-interval-ms:500}") long initialIntervalMs,
            @Value("${app.geo.events.rabbit.retry-multiplier:2.0}") double multiplier,
            @Value("${app.geo.events.rabbit.retry-max-interval-ms:5000}") long maxIntervalMs) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                .maxAttempts(Math.max(1, maxAttempts))
                .backOffOptions(
                        Math.max(100L, initialIntervalMs),
                        Math.max(1.1d, multiplier),
                        Math.max(initialIntervalMs, maxIntervalMs)
                )
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
