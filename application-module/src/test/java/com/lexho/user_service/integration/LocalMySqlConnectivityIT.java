package com.lexho.user_service.integration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("infra-it")
class LocalMySqlConnectivityIT {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/lexho_user_service_it?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "root";

    @Test
    void shouldConnectToConfiguredMySqlThroughHikariAndRunHealthQuery() throws Exception {
        DatabaseSettings settings = DatabaseSettings.load();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("local-mysql-connectivity-it");
        hikariConfig.setJdbcUrl(settings.jdbcUrl());
        hikariConfig.setUsername(settings.username());
        hikariConfig.setPassword(settings.password());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setMinimumIdle(0);
        hikariConfig.setConnectionTimeout(5_000);
        hikariConfig.setValidationTimeout(2_000);
        hikariConfig.setInitializationFailTimeout(5_000);

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig);
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            assertNotNull(connection.getMetaData().getDatabaseProductName());
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    private record DatabaseSettings(String jdbcUrl, String username, String password) {
        private static DatabaseSettings load() throws IOException {
            Map<String, String> dotEnv = loadDotEnv();
            return new DatabaseSettings(
                    resolve("IT_DB_URL", "DB_URL", DEFAULT_URL, dotEnv),
                    resolve("IT_DB_USERNAME", "DB_USERNAME", DEFAULT_USERNAME, dotEnv),
                    resolve("IT_DB_PASSWORD", "DB_PASSWORD", DEFAULT_PASSWORD, dotEnv)
            );
        }

        private static String resolve(String primaryKey,
                                      String secondaryKey,
                                      String defaultValue,
                                      Map<String, String> dotEnv) {
            String systemProperty = System.getProperty(primaryKey);
            if (hasText(systemProperty)) {
                return systemProperty;
            }

            String environmentValue = System.getenv(primaryKey);
            if (hasText(environmentValue)) {
                return environmentValue;
            }

            String primaryDotEnv = dotEnv.get(primaryKey);
            if (hasText(primaryDotEnv)) {
                return primaryDotEnv;
            }

            String secondarySystemProperty = System.getProperty(secondaryKey);
            if (hasText(secondarySystemProperty)) {
                return secondarySystemProperty;
            }

            String secondaryEnvironmentValue = System.getenv(secondaryKey);
            if (hasText(secondaryEnvironmentValue)) {
                return secondaryEnvironmentValue;
            }

            String secondaryDotEnv = dotEnv.get(secondaryKey);
            if (hasText(secondaryDotEnv)) {
                return secondaryDotEnv;
            }

            return defaultValue;
        }

        private static Map<String, String> loadDotEnv() throws IOException {
            Path dotEnvPath = Path.of(".env");
            if (!Files.exists(dotEnvPath)) {
                return Map.of();
            }

            Map<String, String> values = new HashMap<>();
            List<String> lines = Files.readAllLines(dotEnvPath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                values.put(key, value);
            }
            return values;
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
