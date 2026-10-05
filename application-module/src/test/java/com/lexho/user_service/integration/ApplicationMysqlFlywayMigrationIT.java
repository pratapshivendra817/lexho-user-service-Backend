package com.lexho.user_service.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "lexho.it.mysql.application.flyway", matches = "true")
class ApplicationMysqlFlywayMigrationIT {

    private static final MySqlIntegrationSettings SETTINGS = MySqlIntegrationSettings.load();

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        SETTINGS.requireIsolatedDatabase("lexho_application_flyway_test", "Flyway migration testing");
    }

    @Test
    void flywayCreatesCriticalApplicationAndUserSchemaContracts() throws Exception {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .load();

        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isGreaterThan(50);

        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            assertThat(hasColumn(connection, "orders", "assigned_partner_id")).isTrue();
            assertThat(hasColumn(connection, "payouts", "commission_percent")).isTrue();
            assertThat(hasColumn(connection, "payment_refunds", "idempotency_key")).isTrue();
            assertThat(hasColumn(connection, "payment_webhook_events", "status")).isTrue();
            assertThat(hasColumn(connection, "call_log", "session_id")).isTrue();

            assertThat(hasIndex(connection, "orders", "idx_orders_assigned_partner_id")).isTrue();
            assertThat(hasIndex(connection, "payment_transactions", "idx_payment_transactions_booking_created")).isTrue();
            assertThat(hasIndex(connection, "payment_refunds", "idx_payment_refunds_transaction_status")).isTrue();
            assertThat(hasIndex(connection, "payment_webhook_events", "idx_payment_webhook_events_status_retry")).isTrue();
            assertThat(hasIndex(connection, "call_session", "idx_call_session_caller_status")).isTrue();

            assertThat(hasUniqueIndex(connection, "payouts", "uq_payouts_reference_booking")).isTrue();
            assertThat(hasUniqueIndex(connection, "payment_transactions", "uq_payment_transactions_idempotency")).isTrue();
            assertThat(hasUniqueIndex(connection, "payment_refunds", "uq_payment_refunds_idempotency")).isTrue();
            assertThat(hasUniqueIndex(connection, "call_log", "uk_call_log_session_id")).isTrue();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUniqueIndex(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, true, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
