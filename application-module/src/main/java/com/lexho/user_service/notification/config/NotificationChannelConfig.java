package com.lexho.user_service.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationChannelProperties.class)
public class NotificationChannelConfig {
}

