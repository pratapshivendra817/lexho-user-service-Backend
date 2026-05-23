SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'full_name') = 0,
        'ALTER TABLE customers ADD COLUMN full_name VARCHAR(120) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'email') = 0,
        'ALTER TABLE customers ADD COLUMN email VARCHAR(150) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'phone_number') = 0,
        'ALTER TABLE customers ADD COLUMN phone_number VARCHAR(20) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'gender') = 0,
        'ALTER TABLE customers ADD COLUMN gender VARCHAR(10) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'profile_image_url') = 0,
        'ALTER TABLE customers ADD COLUMN profile_image_url VARCHAR(512) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'created_at') = 0,
        'ALTER TABLE customers ADD COLUMN created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'updated_at') = 0,
        'ALTER TABLE customers ADD COLUMN updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customers
SET full_name = COALESCE(full_name, 'Lexho User')
WHERE full_name IS NULL OR TRIM(full_name) = '';

UPDATE customers
SET phone_number = COALESCE(phone_number, CONCAT('unknown-', id))
WHERE phone_number IS NULL OR TRIM(phone_number) = '';
