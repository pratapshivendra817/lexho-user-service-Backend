package com.lexho.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionRuntimeConfigurationTest {

    @Test
    void productionProfilePinsCriticalDatasourceAndShutdownSettings() {
        Properties properties = loadProductionProperties();

        assertThat(properties.getProperty("spring.datasource.hikari.maximum-pool-size")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.minimum-idle")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.connection-timeout")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.validation-timeout")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.idle-timeout")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.max-lifetime")).isNotBlank();
        assertThat(properties.getProperty("spring.datasource.hikari.keepalive-time")).isNotBlank();
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isNotBlank();
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("app.security.websocket.allowed-origins")).isNotBlank();
        assertThat(properties.getProperty("app.security.exposure.allow-partner-request-dev-seed")).isEqualTo("false");
    }

    private Properties loadProductionProperties() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource("application-prod.yml"));
        Properties properties = factoryBean.getObject();
        assertThat(properties).isNotNull();
        return properties;
    }
}
