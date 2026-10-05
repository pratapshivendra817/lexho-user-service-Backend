SET @legacy_partner_category_fk := (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'partners'
      AND column_name = 'service_category_id'
      AND referenced_table_name = 'service_category'
    LIMIT 1
);

SET @drop_legacy_partner_category_fk := IF(
    @legacy_partner_category_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE partners DROP FOREIGN KEY ', @legacy_partner_category_fk)
);

PREPARE stmt FROM @drop_legacy_partner_category_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
