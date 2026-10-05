package com.lexho.user.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "lexho.it.mysql.flyway", matches = "true")
class CallingFlywayMigrationTest {
    private static final String URL = System.getProperty("IT_DB_URL",
            System.getenv().getOrDefault("IT_DB_URL", ""));
    private static final String USERNAME = System.getProperty("IT_DB_USERNAME",
            System.getenv().getOrDefault("IT_DB_USERNAME", ""));
    private static final String PASSWORD = System.getProperty("IT_DB_PASSWORD",
            System.getenv().getOrDefault("IT_DB_PASSWORD", ""));

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        assumeTrue(!URL.isBlank(), "Set IT_DB_URL for isolated MySQL Flyway testing");
        assumeTrue(URL.contains("lexho_user_flyway_test"),
                "IT_DB_URL must point to the isolated lexho_user_flyway_test database");
        assumeTrue(!USERNAME.isBlank(), "Set IT_DB_USERNAME for isolated MySQL Flyway testing");
    }

    @Test
    void flywayCreatesCallingTablesAndForeignKeys() throws Exception {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(URL, USERNAME, PASSWORD)
                .locations("classpath:db/migration/user-service")
                .load();

        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (Connection connection = java.sql.DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            assertThat(hasColumn(connection, "call_session", "status")).isTrue();
            assertThat(hasColumn(connection, "call_session", "duration")).isTrue();
            assertThat(hasColumn(connection, "call_log", "call_type")).isTrue();
            assertThat(hasConstraint(connection, "call_log", "fk_call_log_session")).isTrue();
            assertThat(hasConstraint(connection, "call_log", "uk_call_log_session_id")).isTrue();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private boolean hasConstraint(Connection connection, String tableName, String constraintName) throws Exception {
        try (ResultSet importedKeys = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, tableName)) {
            while (importedKeys.next()) {
                if (constraintName.equalsIgnoreCase(importedKeys.getString("FK_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, true, false)) {
            while (indexes.next()) {
                if (constraintName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
