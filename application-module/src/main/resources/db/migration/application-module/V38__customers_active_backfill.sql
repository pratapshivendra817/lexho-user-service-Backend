SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'active') = 0,
        'ALTER TABLE customers ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customers
SET active = 1
WHERE active IS NULL;
