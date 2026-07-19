SET @cart_addon_signature_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND column_name = 'addon_signature'
);
SET @cart_addon_signature_sql := IF(
    @cart_addon_signature_exists = 0,
    'ALTER TABLE cart_items ADD COLUMN addon_signature VARCHAR(500) NOT NULL DEFAULT ''NONE'' AFTER service_id',
    'SELECT 1'
);
PREPARE stmt FROM @cart_addon_signature_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE cart_items SET addon_signature = 'NONE' WHERE addon_signature IS NULL OR addon_signature = '';

SET @old_cart_unique_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND index_name = 'uk_cart_items_user_service'
);
SET @old_cart_unique_sql := IF(
    @old_cart_unique_exists > 0,
    'DROP INDEX uk_cart_items_user_service ON cart_items',
    'SELECT 1'
);
PREPARE stmt FROM @old_cart_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @new_cart_unique_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND index_name = 'uk_cart_items_user_service_addons'
);
SET @new_cart_unique_sql := IF(
    @new_cart_unique_exists = 0,
    'CREATE UNIQUE INDEX uk_cart_items_user_service_addons ON cart_items(user_id, service_id, addon_signature)',
    'SELECT 1'
);
PREPARE stmt FROM @new_cart_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
