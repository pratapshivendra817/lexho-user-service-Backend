SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'partner_penalties'
      AND COLUMN_NAME = 'dispute_review_note'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN dispute_review_note VARCHAR(1000) NULL',
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
      AND COLUMN_NAME = 'dispute_reviewed_by_admin_id'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN dispute_reviewed_by_admin_id BIGINT NULL',
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
      AND COLUMN_NAME = 'dispute_reviewed_at'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE partner_penalties ADD COLUMN dispute_reviewed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
