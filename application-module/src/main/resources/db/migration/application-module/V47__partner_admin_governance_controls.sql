SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partners' AND COLUMN_NAME = 'availability_locked_until') = 0,
        'ALTER TABLE partners ADD COLUMN availability_locked_until DATETIME NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partners' AND COLUMN_NAME = 'availability_lock_reason') = 0,
        'ALTER TABLE partners ADD COLUMN availability_lock_reason VARCHAR(500) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partners' AND COLUMN_NAME = 'risk_status') = 0,
        'ALTER TABLE partners ADD COLUMN risk_status VARCHAR(30) NULL DEFAULT ''NORMAL''',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partners' AND COLUMN_NAME = 'fraud_flag') = 0,
        'ALTER TABLE partners ADD COLUMN fraud_flag BOOLEAN NOT NULL DEFAULT FALSE',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partners' AND COLUMN_NAME = 'admin_notes') = 0,
        'ALTER TABLE partners ADD COLUMN admin_notes VARCHAR(1000) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE partners SET risk_status = 'NORMAL' WHERE risk_status IS NULL OR TRIM(risk_status) = '';

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'reference_booking_id') = 0,
        'ALTER TABLE payouts ADD COLUMN reference_booking_id BIGINT NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'remarks') = 0,
        'ALTER TABLE payouts ADD COLUMN remarks VARCHAR(500) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'processed_at') = 0,
        'ALTER TABLE payouts ADD COLUMN processed_at DATETIME NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
