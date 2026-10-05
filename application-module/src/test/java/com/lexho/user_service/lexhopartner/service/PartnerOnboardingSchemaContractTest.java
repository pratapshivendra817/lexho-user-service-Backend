package com.lexho.user_service.lexhopartner.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerOnboardingSchemaContractTest {

    @Test
    void partnerOnboardingMigrationContainsFinalRuntimeColumns() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/application-module/V148__partner_onboarding_final_contract.sql"));
        String legacyCompatibility = Files.readString(Path.of(
                "src/main/resources/db/migration/application-module/V149__restore_partner_legacy_profile_validation_columns.sql"));
        String cleanup = Files.readString(Path.of(
                "src/main/resources/db/migration/application-module/V151__remove_unused_partner_onboarding_profile_fields.sql"));

        assertThat(migration)
                .contains("date_of_birth")
                .contains("gender")
                .contains("address_line1")
                .contains("city")
                .contains("state")
                .contains("pincode")
                .contains("document_status")
                .contains("reviewed_by_admin_id")
                .contains("reviewed_at")
                .contains("submitted_at")
                .contains("aadhaar_front_url")
                .contains("aadhaar_back_url")
                .contains("pan_card_url")
                .contains("selfie_url")
                .contains("certificate_url")
                .contains("provider_service_types");

        assertThat(legacyCompatibility)
                .contains("email")
                .contains("current_city")
                .contains("current_state")
                .contains("current_full_address")
                .contains("full_address")
                .contains("profile_image");

        assertThat(cleanup)
                .contains("DROP COLUMN profile_photo_url")
                .contains("DROP COLUMN relationship");
    }
}
