package com.lexho.user_service.map.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MapboxProperties.class)
public class MapboxConfig {
}
