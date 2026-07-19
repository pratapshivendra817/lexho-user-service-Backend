SET @db_name = DATABASE();

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'accuracy_meters') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN accuracy_meters DECIMAL(8,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'heading_degrees') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN heading_degrees DECIMAL(7,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'speed_kmh') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN speed_kmh DECIMAL(8,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'location_seq') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN location_seq BIGINT NOT NULL DEFAULT 0',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'last_heartbeat_at') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN last_heartbeat_at DATETIME NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'fraud_score') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN fraud_score DECIMAL(6,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'fraud_reason') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN fraud_reason VARCHAR(120) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'last_suspicious_at') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN last_suspicious_at DATETIME NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.statistics
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND index_name = 'idx_current_partner_locations_seq') = 0,
        'CREATE INDEX idx_current_partner_locations_seq ON current_partner_locations (partner_id, location_seq)',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
