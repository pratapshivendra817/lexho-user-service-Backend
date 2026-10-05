CREATE TABLE IF NOT EXISTS dynamic_content_translation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    field_name VARCHAR(120) NOT NULL,
    source_locale VARCHAR(10) NOT NULL,
    target_locale VARCHAR(10) NOT NULL,
    source_text_hash CHAR(64) NOT NULL,
    translated_text TEXT NOT NULL,
    translation_provider VARCHAR(80) NOT NULL,
    translation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_dynamic_translation_source UNIQUE (
        entity_type,
        entity_id,
        field_name,
        source_locale,
        target_locale,
        source_text_hash
    )
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_dynamic_translation_lookup
    ON dynamic_content_translation (
        entity_type,
        entity_id,
        field_name,
        source_locale,
        target_locale,
        source_text_hash,
        translation_status
    );

CREATE INDEX idx_dynamic_translation_target_locale
    ON dynamic_content_translation (target_locale, translation_status);

CREATE INDEX idx_dynamic_translation_updated_at
    ON dynamic_content_translation (updated_at);
