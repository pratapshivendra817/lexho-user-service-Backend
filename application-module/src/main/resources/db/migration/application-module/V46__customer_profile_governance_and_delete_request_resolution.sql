SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'profile_edit_locked') = 0,
        'ALTER TABLE customers ADD COLUMN profile_edit_locked TINYINT(1) NULL DEFAULT 0',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'email_verified') = 0,
        'ALTER TABLE customers ADD COLUMN email_verified TINYINT(1) NULL DEFAULT 0',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'phone_verified') = 0,
        'ALTER TABLE customers ADD COLUMN phone_verified TINYINT(1) NULL DEFAULT 0',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'profile_image_approved') = 0,
        'ALTER TABLE customers ADD COLUMN profile_image_approved TINYINT(1) NULL DEFAULT 1',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'admin_notes') = 0,
        'ALTER TABLE customers ADD COLUMN admin_notes VARCHAR(1000) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customers SET profile_edit_locked = 0 WHERE profile_edit_locked IS NULL;
UPDATE customers SET email_verified = 0 WHERE email_verified IS NULL;
UPDATE customers SET phone_verified = 0 WHERE phone_verified IS NULL;
UPDATE customers SET profile_image_approved = 1 WHERE profile_image_approved IS NULL;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer_account_delete_requests' AND COLUMN_NAME = 'resolution_status') = 0,
        'ALTER TABLE customer_account_delete_requests ADD COLUMN resolution_status VARCHAR(20) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer_account_delete_requests' AND COLUMN_NAME = 'resolution_reason') = 0,
        'ALTER TABLE customer_account_delete_requests ADD COLUMN resolution_reason VARCHAR(500) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer_account_delete_requests' AND COLUMN_NAME = 'resolved_by_admin_id') = 0,
        'ALTER TABLE customer_account_delete_requests ADD COLUMN resolved_by_admin_id BIGINT NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer_account_delete_requests' AND COLUMN_NAME = 'resolved_at') = 0,
        'ALTER TABLE customer_account_delete_requests ADD COLUMN resolved_at TIMESTAMP NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customer_account_delete_requests
SET resolution_status = CASE WHEN resolved = 1 THEN 'APPROVED' ELSE 'PENDING' END
WHERE resolution_status IS NULL;
