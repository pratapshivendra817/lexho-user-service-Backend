package com.lexho.user_service.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "lexho.it.mysql.production.readiness", matches = "true")
class ApplicationMysqlProductionReadinessIT {

    private static final MySqlIntegrationSettings SETTINGS = MySqlIntegrationSettings.load();

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        SETTINGS.requireIsolatedDatabase("lexho_application_production_readiness_test", "production readiness testing");
    }

    @Test
    void freshMysqlMigrationProducesProductionReadySchemaAndAppStartup() throws Exception {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .load();

        flyway.clean();
        int executed = flyway.migrate().migrationsExecuted;
        assertThat(executed).isGreaterThan(50);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(Arrays.stream(flyway.info().all()).map(MigrationInfo::getState))
                .noneMatch(state -> state.isFailed());

        try (Connection connection = DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())) {
            assertThat(MySqlSchemaInspector.hasTable(connection, "orders")).isTrue();
            assertThat(MySqlSchemaInspector.hasTable(connection, "payment_transactions")).isTrue();
            assertThat(MySqlSchemaInspector.hasTable(connection, "payment_refunds")).isTrue();
            assertThat(MySqlSchemaInspector.hasTable(connection, "payouts")).isTrue();
            assertThat(MySqlSchemaInspector.hasTable(connection, "call_session")).isTrue();

            assertThat(MySqlSchemaInspector.hasPrimaryKey(connection, "orders", "id")).isTrue();
            assertThat(MySqlSchemaInspector.hasPrimaryKey(connection, "payment_transactions", "id")).isTrue();

            assertThat(MySqlSchemaInspector.hasForeignKey(connection, "payment_transactions", "fk_payment_transactions_booking")).isTrue();
            assertThat(MySqlSchemaInspector.hasForeignKey(connection, "payment_refunds", "fk_payment_refunds_transaction")).isTrue();
            assertThat(MySqlSchemaInspector.hasForeignKey(connection, "partner_settlement_refunds", "fk_partner_settlement_refund_payout")).isTrue();

            assertThat(MySqlSchemaInspector.hasUniqueIndex(connection, "payment_transactions", "uq_payment_transactions_idempotency")).isTrue();
            assertThat(MySqlSchemaInspector.hasUniqueIndex(connection, "payment_refunds", "uq_payment_refunds_idempotency")).isTrue();
            assertThat(MySqlSchemaInspector.hasUniqueIndex(connection, "payouts", "uq_payouts_reference_booking")).isTrue();

            assertThat(MySqlSchemaInspector.hasIndex(connection, "payment_transactions", "idx_payment_transactions_booking_created")).isTrue();
            assertThat(MySqlSchemaInspector.hasIndex(connection, "payment_webhook_events", "idx_payment_webhook_events_status_retry")).isTrue();
            assertThat(MySqlSchemaInspector.hasIndex(connection, "payment_refunds", "idx_payment_refunds_transaction_status")).isTrue();
            assertThat(MySqlSchemaInspector.dataType(connection, "customer_addresses", "id")).isEqualToIgnoringCase("BINARY");
            assertThat(MySqlSchemaInspector.dataType(connection, "customer_current_locations", "id")).isEqualToIgnoringCase("BINARY");

            assertThat(MySqlSchemaInspector.isNullable(connection, "payment_transactions", "idempotency_key")).isFalse();
            assertThat(MySqlSchemaInspector.isNullable(connection, "payment_refunds", "refund_status")).isFalse();
            assertThat(MySqlSchemaInspector.isNullable(connection, "payouts", "commission_percent")).isFalse();

            assertThat(MySqlSchemaInspector.defaultValue(connection, "payment_transactions", "currency")).containsIgnoringCase("INR");
            assertThat(MySqlSchemaInspector.defaultValue(connection, "payment_refunds", "refund_status")).containsIgnoringCase("REQUESTED");
            assertThat(MySqlSchemaInspector.defaultValue(connection, "payouts", "commission_percent")).contains("15.00");
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
}
