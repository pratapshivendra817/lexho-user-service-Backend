SET @add_account_holder_name = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'account_holder_name'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN account_holder_name VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @add_account_holder_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_bank_name = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'bank_name'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN bank_name VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @add_bank_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_branch_name = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'branch_name'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN branch_name VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @add_branch_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_upi_id = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'upi_id'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN upi_id VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @add_upi_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS provider_service_types (
    provider_id BIGINT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    CONSTRAINT fk_provider_service_types_provider
        FOREIGN KEY (provider_id) REFERENCES partners(id)
);
