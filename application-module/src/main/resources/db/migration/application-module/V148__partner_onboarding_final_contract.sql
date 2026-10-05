SET @add_date_of_birth = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'date_of_birth'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN date_of_birth DATE NULL'
    )
);
PREPARE stmt FROM @add_date_of_birth;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_gender = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'gender'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN gender VARCHAR(20) NULL'
    )
);
PREPARE stmt FROM @add_gender;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_profile_photo_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'profile_photo_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN profile_photo_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_profile_photo_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_address_line1 = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'address_line1'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN address_line1 VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @add_address_line1;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_city = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'city'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN city VARCHAR(100) NULL'
    )
);
PREPARE stmt FROM @add_city;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_state = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'state'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN state VARCHAR(100) NULL'
    )
);
PREPARE stmt FROM @add_state;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_pincode = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'pincode'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN pincode VARCHAR(20) NULL'
    )
);
PREPARE stmt FROM @add_pincode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_document_status = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'document_status'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN document_status VARCHAR(30) NOT NULL DEFAULT ''PENDING'''
    )
);
PREPARE stmt FROM @add_document_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_reviewed_by_admin_id = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'reviewed_by_admin_id'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN reviewed_by_admin_id BIGINT NULL'
    )
);
PREPARE stmt FROM @add_reviewed_by_admin_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_reviewed_at = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'reviewed_at'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN reviewed_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @add_reviewed_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_submitted_at = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'submitted_at'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN submitted_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @add_submitted_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_aadhaar_front_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'aadhaar_front_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN aadhaar_front_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_aadhaar_front_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_aadhaar_back_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'aadhaar_back_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN aadhaar_back_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_aadhaar_back_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_pan_card_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'pan_card_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN pan_card_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_pan_card_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_selfie_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'selfie_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN selfie_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_selfie_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_certificate_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'certificate_url'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN certificate_url VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_certificate_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_relationship = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'relationship'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN relationship VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @add_relationship;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS provider_service_types (
    provider_id BIGINT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    CONSTRAINT fk_provider_service_types_provider
        FOREIGN KEY (provider_id) REFERENCES partners(id)
);
