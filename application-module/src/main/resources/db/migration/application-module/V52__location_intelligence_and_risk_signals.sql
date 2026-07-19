SET @db_name = DATABASE();

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'manual_confirmed') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN manual_confirmed BOOLEAN NOT NULL DEFAULT FALSE',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'accuracy_meters') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN accuracy_meters DECIMAL(8,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'confidence_score') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN confidence_score DECIMAL(5,4) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'location_age_seconds') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN location_age_seconds INT NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'detection_source') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN detection_source VARCHAR(32) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'mock_location') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN mock_location BOOLEAN NOT NULL DEFAULT FALSE',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'suspicious_jump') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN suspicious_jump BOOLEAN NOT NULL DEFAULT FALSE',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'spoof_attempt_count') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN spoof_attempt_count INT NOT NULL DEFAULT 0',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'risk_score') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN risk_score DECIMAL(5,2) NOT NULL DEFAULT 0.00',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'last_idempotency_key') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN last_idempotency_key VARCHAR(128) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND column_name = 'last_location_fingerprint') = 0,
        'ALTER TABLE customer_current_locations ADD COLUMN last_location_fingerprint VARCHAR(128) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.statistics
         WHERE table_schema = @db_name
           AND table_name = 'customer_current_locations'
           AND index_name = 'idx_customer_current_locations_manual_confirmed') = 0,
        'CREATE INDEX idx_customer_current_locations_manual_confirmed ON customer_current_locations (manual_confirmed)',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
