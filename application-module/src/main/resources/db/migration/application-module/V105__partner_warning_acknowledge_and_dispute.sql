SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'partner_penalties'
      AND COLUMN_NAME = 'acknowledged_at'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN acknowledged_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'partner_penalties'
      AND COLUMN_NAME = 'dispute_status'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN dispute_status VARCHAR(30) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'partner_penalties'
      AND COLUMN_NAME = 'dispute_reason'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN dispute_reason VARCHAR(1000) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'partner_penalties'
      AND COLUMN_NAME = 'disputed_at'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN disputed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
