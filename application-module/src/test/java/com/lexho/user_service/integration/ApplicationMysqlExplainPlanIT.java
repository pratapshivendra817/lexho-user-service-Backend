package com.lexho.user_service.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "lexho.it.mysql.explain", matches = "true")
class ApplicationMysqlExplainPlanIT {

    private static final MySqlIntegrationSettings SETTINGS = MySqlIntegrationSettings.load();

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        SETTINGS.requireIsolatedDatabase("lexho_application_explain_test", "EXPLAIN plan testing");
    }

    @Test
    void criticalQueriesUseExpectedIndexesOnMysql() throws Exception {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .load();

        flyway.clean();
        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            long orderId = insertOrder(connection);
            insertPaymentTransaction(connection, orderId);
            insertWebhookEvent(connection);
            insertCallSession(connection);

            assertThat(explainKey(connection,
                    "EXPLAIN SELECT id FROM orders WHERE assigned_partner_id = ?",
                    88L)).isEqualToIgnoringCase("idx_orders_assigned_partner_id");
            assertThat(explainKey(connection,
                    "EXPLAIN SELECT id FROM payment_transactions WHERE booking_id = ? ORDER BY created_at DESC",
                    orderId)).isEqualToIgnoringCase("idx_payment_transactions_booking_created");
            assertThat(explainKey(connection,
                    "EXPLAIN SELECT id FROM payment_webhook_events WHERE status = ? AND next_retry_at < ?",
                    "PENDING", Timestamp.valueOf(LocalDateTime.now().plusMinutes(5))))
                    .isEqualToIgnoringCase("idx_payment_webhook_events_status_retry");
            assertThat(explainKey(connection,
                    "EXPLAIN SELECT id FROM call_session WHERE caller_id = ? AND status = ?",
                    77L, "CONNECTED")).isEqualToIgnoringCase("idx_call_session_caller_status");
        }
    }

    private long insertOrder(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO orders (
                    user_id, total_amount, status, payment_status, address, scheduled_at, created_at, updated_at,
                    customer_name, customer_phone, latitude, longitude, service_type, amount, payment_mode,
                    assigned_partner_id, booking_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, 77L);
            statement.setDouble(2, 1200.0);
            statement.setString(3, "COMPLETED");
            statement.setString(4, "SUCCESS");
            statement.setString(5, "Test Address");
            statement.setTimestamp(6, Timestamp.valueOf(now.plusDays(1)));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setString(9, "Customer");
            statement.setString(10, "9999999999");
            statement.setDouble(11, 28.61);
            statement.setDouble(12, 77.20);
            statement.setString(13, "Cleaning");
            statement.setDouble(14, 1200.0);
            statement.setString(15, "ONLINE");
            statement.setLong(16, 88L);
            statement.setString(17, "INSTANT");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private void insertPaymentTransaction(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payment_transactions (
                    booking_id, customer_id, idempotency_key, razorpay_order_id,
                    amount, currency, payment_status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, orderId);
            statement.setLong(2, 77L);
            statement.setString(3, "explain-idem-" + orderId);
            statement.setString(4, "order-" + orderId);
            statement.setBigDecimal(5, new BigDecimal("1200.00"));
            statement.setString(6, "INR");
            statement.setString(7, "SUCCESS");
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private void insertWebhookEvent(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payment_webhook_events (
                    event_id, event_type, idempotency_key, status, received_at, next_retry_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setString(1, "evt-explain-1");
            statement.setString(2, "payment.captured");
            statement.setString(3, "evt-explain-idem-1");
            statement.setString(4, "PENDING");
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.setTimestamp(6, Timestamp.valueOf(now.plusMinutes(1)));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private void insertCallSession(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO call_session (
                    caller_id, receiver_id, status, start_time, duration
                ) VALUES (?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, 77L);
            statement.setLong(2, 88L);
            statement.setString(3, "CONNECTED");
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setLong(5, 0L);
            statement.executeUpdate();
        }
    }

    private String explainKey(Connection connection, String sql, Object... args) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString("key");
            }
        }
    }
}
