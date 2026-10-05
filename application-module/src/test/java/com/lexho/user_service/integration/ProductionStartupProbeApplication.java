package com.lexho.user_service.integration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@SpringBootApplication
@EntityScan(basePackages = "com.lexho")
@EnableJpaRepositories(basePackages = "com.lexho")
class ProductionStartupProbeApplication {

    @Bean
    @Primary
    DataSource productionStartupProbeDataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requiredProperty(environment, "lexho.test.jdbc.url", "spring.datasource.url"));
        config.setUsername(requiredProperty(environment, "lexho.test.jdbc.username", "spring.datasource.username"));
        config.setPassword(requiredProperty(environment, "lexho.test.jdbc.password", "spring.datasource.password"));
        config.setDriverClassName(environment.getProperty("lexho.test.jdbc.driver",
                environment.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver")));
        config.setPoolName("production-startup-probe");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);
        return new HikariDataSource(config);
    }

    private String requiredProperty(Environment environment, String primaryKey, String fallbackKey) {
        String value = environment.getProperty(primaryKey);
        if (value == null || value.isBlank()) {
            value = environment.getProperty(fallbackKey);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing datasource property: " + primaryKey);
        }
        return value;
    }
}
