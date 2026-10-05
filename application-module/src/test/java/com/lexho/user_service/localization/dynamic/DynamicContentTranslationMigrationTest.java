package com.lexho.user_service.localization.dynamic;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicContentTranslationMigrationTest {

    @Test
    void migrationDefinesRepositoryConstraintsAndIndexes() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/application-module/V123__dynamic_content_translation.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS dynamic_content_translation");
        assertThat(migration).contains("CONSTRAINT uk_dynamic_translation_source UNIQUE");
        assertThat(migration).contains("entity_type");
        assertThat(migration).contains("entity_id");
        assertThat(migration).contains("field_name");
        assertThat(migration).contains("source_locale");
        assertThat(migration).contains("target_locale");
        assertThat(migration).contains("source_text_hash");
        assertThat(migration).contains("CREATE INDEX idx_dynamic_translation_lookup");
        assertThat(migration).contains("CREATE INDEX idx_dynamic_translation_target_locale");
    }
}
