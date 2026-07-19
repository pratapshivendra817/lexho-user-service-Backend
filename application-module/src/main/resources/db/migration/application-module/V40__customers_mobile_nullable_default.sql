SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'mobile') = 0,
        'ALTER TABLE customers ADD COLUMN mobile VARCHAR(20) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE customers
    MODIFY COLUMN mobile VARCHAR(20) NULL DEFAULT NULL;

UPDATE customers
SET mobile = COALESCE(NULLIF(phone_number, ''), mobile)
WHERE mobile IS NULL OR TRIM(mobile) = '';
