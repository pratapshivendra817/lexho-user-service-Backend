SET @family_size_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND column_name = 'family_size_id'
);
SET @family_size_id_sql := IF(
    @family_size_id_exists = 0,
    'ALTER TABLE cart_items ADD COLUMN family_size_id BIGINT NULL AFTER addon_signature',
    'SELECT 1'
);
PREPARE stmt FROM @family_size_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @family_size_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND column_name = 'family_size'
);
SET @family_size_sql := IF(
    @family_size_exists = 0,
    'ALTER TABLE cart_items ADD COLUMN family_size VARCHAR(30) NULL AFTER family_size_id',
    'SELECT 1'
);
PREPARE stmt FROM @family_size_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @family_size_label_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND column_name = 'family_size_label'
);
SET @family_size_label_sql := IF(
    @family_size_label_exists = 0,
    'ALTER TABLE cart_items ADD COLUMN family_size_label VARCHAR(100) NULL AFTER family_size',
    'SELECT 1'
);
PREPARE stmt FROM @family_size_label_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE cart_items
SET addon_signature = CONCAT('FAMILY:NONE::', addon_signature)
WHERE addon_signature IS NOT NULL
  AND addon_signature NOT LIKE 'FAMILY:%';
