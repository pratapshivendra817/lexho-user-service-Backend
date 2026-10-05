package com.lexho.user_service.integration;

import org.junit.jupiter.api.Assumptions;

import java.sql.DriverManager;
import java.sql.Statement;

final class MySqlIntegrationSettings {

    private final String url;
    private final String username;
    private final String password;

    private MySqlIntegrationSettings(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    static MySqlIntegrationSettings load() {
        return new MySqlIntegrationSettings(
                property("IT_DB_URL"),
                property("IT_DB_USERNAME"),
                property("IT_DB_PASSWORD")
        );
    }

    void requireIsolatedDatabase(String requiredName, String purpose) {
        Assumptions.assumeTrue(!url.isBlank(), "Set IT_DB_URL for isolated MySQL " + purpose);
        Assumptions.assumeTrue(url.contains(requiredName),
                "IT_DB_URL must point to the isolated " + requiredName + " database");
        Assumptions.assumeTrue(!username.isBlank(), "Set IT_DB_USERNAME for isolated MySQL " + purpose);
    }

    String url() {
        return url;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    void resetDatabase() throws Exception {
        try (var connection = DriverManager.getConnection(adminUrl(), username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + databaseName() + "`");
            statement.execute("CREATE DATABASE `" + databaseName() + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static String property(String key) {
        return System.getProperty(key, System.getenv().getOrDefault(key, ""));
    }

    private String adminUrl() {
        int jdbcPrefixEnd = url.indexOf("://");
        if (jdbcPrefixEnd < 0) {
            throw new IllegalStateException("Unsupported JDBC URL " + url);
        }

        int databaseStart = url.indexOf('/', jdbcPrefixEnd + 3);
        if (databaseStart < 0) {
            throw new IllegalStateException("Unable to derive admin JDBC URL from " + url);
        }

        int queryStart = url.indexOf('?', databaseStart);
        String suffix = queryStart >= 0 ? url.substring(queryStart) : "";
        return url.substring(0, databaseStart) + "/mysql" + suffix;
    }

    private String databaseName() {
        int databaseStart = url.lastIndexOf('/');
        if (databaseStart < 0 || databaseStart == url.length() - 1) {
            throw new IllegalStateException("Unable to derive database name from " + url);
        }

        int queryStart = url.indexOf('?', databaseStart);
        return queryStart >= 0 ? url.substring(databaseStart + 1, queryStart) : url.substring(databaseStart + 1);
    }
}
