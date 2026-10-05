package com.lexho.user_service.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationModuleFlywayMigrationTest {

    @Test
    void criticalSchemaContractsRemainRepresentedInMigrationSources() throws Exception {
        assertThat(migration("application-module", "V55__serviceability_rules_table.sql"))
                .contains("polygon_json LONGTEXT NULL")
                .contains("city_id BIGINT NULL");
        assertThat(migration("application-module", "V44__city_zone_and_service_area_geofence.sql"))
                .contains("fk_serviceable_areas_city");
        assertThat(migration("application-module", "V56__partner_geo_spatial_mysql_and_indexes.sql"))
                .contains("ADD COLUMN geo_point POINT SRID 4326 NULL")
                .contains("idx_current_partner_locations_geo_point");
        assertThat(migration("application-module", "V57__partner_tracking_optimization_columns.sql"))
                .contains("ADD COLUMN location_seq BIGINT NOT NULL DEFAULT 0")
                .contains("idx_current_partner_locations_seq");
        assertThat(migration("application-module", "V104__support_ticket_two_way_reply_and_attachments.sql"))
                .contains("ADD COLUMN sender_type VARCHAR(20) NOT NULL DEFAULT 'SUPPORT'");
        assertThat(migration("application-module", "V108__lexho_auth_state_hardening.sql"))
                .contains("auth_refresh_tokens")
                .contains("role VARCHAR(20) NOT NULL")
                .contains("otp_hash VARCHAR(255) NOT NULL")
                .contains("token VARCHAR(64) NOT NULL")
                .contains("idx_lexho_login_otps_lookup");
        assertThat(migration("application-module", "V109__payout_reference_booking_uniqueness.sql"))
                .contains("uq_payouts_reference_booking")
                .contains("ALTER TABLE payouts");
        assertThat(migration("application-module", "V110__normalize_refresh_token_hash_columns.sql"))
                .contains("ALTER TABLE lexho_refresh_tokens")
                .contains("MODIFY COLUMN token VARCHAR(64) NOT NULL")
                .contains("ALTER TABLE auth_refresh_tokens");
        assertThat(migration("application-module", "V111__restore_banner_promotion_contract.sql"))
                .contains("ADD COLUMN placement VARCHAR(20) NOT NULL DEFAULT ''TOP''")
                .contains("ADD COLUMN redirect_type VARCHAR(30) NOT NULL DEFAULT ''NONE''")
                .contains("ADD COLUMN redirect_value VARCHAR(1000) NULL");
        assertThat(migration("application-module", "V112__order_runtime_schema_completion.sql"))
                .contains("CREATE TABLE IF NOT EXISTS cart")
                .contains("CREATE TABLE IF NOT EXISTS order_items")
                .contains("fk_order_items_order");
        assertThat(migration("application-module", "V113__customers_city_alignment.sql"))
                .contains("ADD COLUMN city VARCHAR(100) NULL");
        assertThat(migration("application-module", "V114__lexho_runtime_schema_completion.sql"))
                .contains("CREATE TABLE IF NOT EXISTS lexho_user_profiles")
                .contains("CREATE TABLE IF NOT EXISTS lexho_favorites")
                .contains("CREATE TABLE IF NOT EXISTS lexho_referral_codes")
                .contains("CREATE TABLE IF NOT EXISTS lexho_referral_uses");
        assertThat(migration("application-module", "V115__legacy_partner_compatibility_table.sql"))
                .contains("CREATE TABLE IF NOT EXISTS partner")
                .contains("idx_partner_sub_category_available")
                .contains("FROM partners p");
        assertThat(migration("application-module", "V116__partners_runtime_contract_alignment.sql"))
                .contains("column_name = 'is_online'")
                .contains("ALTER TABLE partners ADD COLUMN rating DOUBLE NOT NULL DEFAULT 0.0")
                .contains("LEFT JOIN service_category sc ON sc.id = p.service_category_id");
        assertThat(migration("application-module", "V117__sub_category_catalog_contract.sql"))
                .contains("CREATE TABLE IF NOT EXISTS sub_category")
                .contains("fk_sub_category_category");
        assertThat(migration("application-module", "V118__customer_location_uuid_binary_alignment.sql"))
                .contains("ALTER TABLE customer_addresses ADD COLUMN id_bin BINARY(16) NULL FIRST")
                .contains("UNHEX(REPLACE(id, '-', ''))")
                .contains("ALTER TABLE customer_current_locations ADD COLUMN id_bin BINARY(16) NULL FIRST");
        assertThat(migration("application-module", "V122__create_office_locations.sql"))
                .contains("CREATE TABLE IF NOT EXISTS office_locations")
                .contains("active_type VARCHAR(50) GENERATED ALWAYS AS")
                .contains("uk_office_locations_one_active_type")
                .contains("idx_office_locations_type_active");
        assertThat(migration("application-module", "V131__social_media_links.sql"))
                .contains("CREATE TABLE social_media_links")
                .contains("uk_social_media_links_platform")
                .contains("idx_social_media_links_active_order")
                .contains("CHECK (platform IN ('INSTAGRAM', 'FACEBOOK', 'YOUTUBE', 'LINKEDIN', 'X', 'OTHER'))");
        assertThat(migration("application-module", "V81__production_observability_audit_contract.sql"))
                .contains("uq_payment_webhook_events_idempotency_key");
        assertThat(migration("application-module", "V59__payment_refunds.sql"))
                .contains("uq_payment_refunds_idempotency")
                .contains("idx_payment_refunds_transaction_status");
        assertThat(migration("user-service", "V87__create_calling_tables.sql"))
                .contains("idx_call_session_caller_status");
    }

    private String migration(String module, String fileName) {
        Path path = switch (module) {
            case "application-module" -> Path.of("src", "main", "resources", "db", "migration", "application-module", fileName);
            case "user-service" -> Path.of("..", "user-service", "src", "main", "resources", "db", "migration", "user-service", fileName);
            default -> throw new IllegalArgumentException("Unsupported module " + module);
        };
        return read(path);
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read migration " + path, ex);
        }
    }
}
