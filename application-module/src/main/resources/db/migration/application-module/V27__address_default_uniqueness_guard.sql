-- Ensure only one active default address per customer at DB level.
-- Uses a generated guard column so only rows with (is_default=1 and is_deleted=0)
-- participate in uniqueness.

SET @has_guard_column := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'default_active_user_id'
);

SET @add_guard_column_sql := IF(
    @has_guard_column = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN default_active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_default = 1 AND is_deleted = 0 THEN user_id ELSE NULL END) STORED',
    'SELECT 1'
);
PREPARE stmt FROM @add_guard_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_guard_unique := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND index_name = 'uq_lexho_addresses_one_active_default'
);

SET @add_guard_unique_sql := IF(
    @has_guard_unique = 0,
    'CREATE UNIQUE INDEX uq_lexho_addresses_one_active_default ON lexho_addresses(default_active_user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_guard_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
