package com.lexho.user_service.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "lexho.it.mysql.application.upgrade", matches = "true")
class ApplicationMysqlUpgradeMigrationIT {

    private static final MySqlIntegrationSettings SETTINGS = MySqlIntegrationSettings.load();

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        SETTINGS.requireIsolatedDatabase("lexho_application_upgrade_test", "upgrade migration testing");
    }

    @Test
    void upgradeFromProductionLikeSchemaPreservesCriticalDataAndAllowsStartup() throws Exception {
        Flyway flyway = baseFlyway();
        SETTINGS.resetDatabase();

        Flyway legacy = Flyway.configure()
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .target("108")
                .load();
        legacy.migrate();

        long orderId;
        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            orderId = insertLegacyOrder(connection);
            insertLegacyPaymentTransaction(connection, orderId);
            insertLegacyPayout(connection, orderId);
        }

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(1);

        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            assertThat(count(connection, "SELECT COUNT(*) FROM orders WHERE id = " + orderId)).isEqualTo(1L);
            assertThat(count(connection, "SELECT COUNT(*) FROM payment_transactions WHERE booking_id = " + orderId)).isEqualTo(1L);
            assertThat(count(connection, "SELECT COUNT(*) FROM payouts WHERE reference_booking_id = " + orderId)).isEqualTo(1L);
            assertThat(count(connection, "SELECT COUNT(*) FROM payouts WHERE reference_booking_id = " + orderId + " AND provider_id = 90")).isEqualTo(1L);
            assertThat(MySqlSchemaInspector.hasUniqueIndex(connection, "payouts", "uq_payouts_reference_booking")).isTrue();
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ProductionStartupProbeApplication.class)
                .profiles("prod")
                .properties(
                        "DB_URL=" + SETTINGS.url(),
                        "DB_USERNAME=" + SETTINGS.username(),
                        "DB_PASSWORD=" + SETTINGS.password(),
                        "lexho.test.jdbc.url=" + SETTINGS.url(),
                        "lexho.test.jdbc.username=" + SETTINGS.username(),
                        "lexho.test.jdbc.password=" + SETTINGS.password(),
                        "lexho.test.jdbc.driver=com.mysql.cj.jdbc.Driver",
                        "spring.datasource.url=" + SETTINGS.url(),
                        "spring.datasource.username=" + SETTINGS.username(),
                        "spring.datasource.password=" + SETTINGS.password(),
                        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                        "jakarta.persistence.jdbc.url=" + SETTINGS.url(),
                        "jakarta.persistence.jdbc.user=" + SETTINGS.username(),
                        "jakarta.persistence.jdbc.password=" + SETTINGS.password(),
                        "jakarta.persistence.jdbc.driver=com.mysql.cj.jdbc.Driver",
                        "security.jwt.secret=abcdefghijklmnopqrstuvwxyz123456",
                        "INTERNAL_API_TOKEN=internal-token-abcdefghijklmnopqrstuvwxyz",
                        "APP_ALLOWED_CORS_ORIGINS=https://app.lexho.example",
                        "app.push.fcm.enabled=false",
                        "app.notification.email.enabled=false",
                        "app.notification.sms.enabled=false",
                        "spring.main.web-application-type=none",
                        "spring.main.allow-bean-definition-overriding=true",
                        "spring.task.scheduling.enabled=false",
                        "spring.flyway.enabled=false",
                        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                        "spring.jpa.hibernate.ddl-auto=validate"
                )
                .run()) {
            assertThat(context.isActive()).isTrue();
        }
    }

    @Test
    void upgradeFailsWhenPreExistingDuplicatePayoutsWouldViolateCurrentUniquenessRule() throws Exception {
        Flyway flyway = baseFlyway();
        SETTINGS.resetDatabase();

        Flyway legacy = Flyway.configure()
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .target("108")
                .load();
        legacy.migrate();

        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            long orderId = insertLegacyOrder(connection);
            insertLegacyPayout(connection, orderId);
            insertLegacyPayout(connection, orderId);
        }

        assertThatThrownBy(flyway::migrate)
                .hasMessageContaining("uq_payouts_reference_booking");
    }

    private Flyway baseFlyway() {
        return Flyway.configure()
                .cleanDisabled(false)
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .load();
    }

    private long insertLegacyOrder(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO orders (
                    user_id, total_amount, status, payment_status, address, scheduled_at, created_at, updated_at,
                    customer_name, customer_phone, latitude, longitude, service_type, amount, payment_mode, booking_type
                ) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), NOW(), ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, 77L);
            statement.setDouble(2, 1250.0);
            statement.setString(3, "COMPLETED");
            statement.setString(4, "SUCCESS");
            statement.setString(5, "Legacy upgrade address");
            statement.setString(6, "Customer");
            statement.setString(7, "9999999999");
            statement.setDouble(8, 28.61);
            statement.setDouble(9, 77.20);
            statement.setString(10, "Cleaning");
            statement.setDouble(11, 1250.0);
            statement.setString(12, "ONLINE");
            statement.setString(13, "INSTANT");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private void insertLegacyPaymentTransaction(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payment_transactions (
                    booking_id, customer_id, idempotency_key, razorpay_order_id, amount, currency,
                    payment_status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """)) {
            statement.setLong(1, orderId);
            statement.setLong(2, 77L);
            statement.setString(3, "legacy-idem-" + orderId);
            statement.setString(4, "legacy-order-" + orderId);
            statement.setBigDecimal(5, new BigDecimal("1250.00"));
            statement.setString(6, "INR");
            statement.setString(7, "PAID");
            statement.executeUpdate();
        }
    }

    private void insertLegacyPayout(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payouts (
                    provider_id, amount, gross_amount, commission_percent, commission_amount, currency,
                    reversed_amount, status, reference_booking_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """)) {
            statement.setLong(1, 90L);
            statement.setDouble(2, 1062.5);
            statement.setBigDecimal(3, new BigDecimal("1250.00"));
            statement.setBigDecimal(4, new BigDecimal("15.00"));
            statement.setBigDecimal(5, new BigDecimal("187.50"));
            statement.setString(6, "INR");
            statement.setBigDecimal(7, BigDecimal.ZERO);
            statement.setString(8, "PENDING");
            statement.setLong(9, orderId);
            statement.executeUpdate();
        }
    }

    private long count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }
}
