SET @has_deleted_at := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND column_name = 'deleted_at'
);
SET @add_deleted_at_sql := IF(
    @has_deleted_at = 0,
    'ALTER TABLE popular_services ADD COLUMN deleted_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @add_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_deleted_by := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND column_name = 'deleted_by'
);
SET @add_deleted_by_sql := IF(
    @has_deleted_by = 0,
    'ALTER TABLE popular_services ADD COLUMN deleted_by BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @add_deleted_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_delete_reason := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND column_name = 'delete_reason'
);
SET @add_delete_reason_sql := IF(
    @has_delete_reason = 0,
    'ALTER TABLE popular_services ADD COLUMN delete_reason VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @add_delete_reason_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_popular_services_deleted_at_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'idx_popular_services_deleted_at'
);
SET @idx_popular_services_deleted_at_sql := IF(
    @idx_popular_services_deleted_at_exists = 0,
    'CREATE INDEX idx_popular_services_deleted_at ON popular_services(deleted_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_popular_services_deleted_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
