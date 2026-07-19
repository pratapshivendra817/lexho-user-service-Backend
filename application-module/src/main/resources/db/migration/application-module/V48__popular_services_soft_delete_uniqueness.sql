SET @has_idx_service_id := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'idx_popular_services_service_id'
);
SET @create_idx_service_id_sql := IF(
    @has_idx_service_id = 0,
    'CREATE INDEX idx_popular_services_service_id ON popular_services(service_id)',
    'SELECT 1'
);
PREPARE stmt FROM @create_idx_service_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_category_id := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'idx_popular_services_category_id'
);
SET @create_idx_category_id_sql := IF(
    @has_idx_category_id = 0,
    'CREATE INDEX idx_popular_services_category_id ON popular_services(category_id)',
    'SELECT 1'
);
PREPARE stmt FROM @create_idx_category_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_service_category := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'uk_popular_services_service_category'
      AND constraint_type = 'UNIQUE'
);
SET @drop_uk_service_category_sql := IF(
    @has_uk_service_category = 1,
    'ALTER TABLE popular_services DROP INDEX uk_popular_services_service_category',
    'SELECT 1'
);
PREPARE stmt FROM @drop_uk_service_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_category_priority := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'uk_popular_services_category_priority'
      AND constraint_type = 'UNIQUE'
);
SET @drop_uk_category_priority_sql := IF(
    @has_uk_category_priority = 1,
    'ALTER TABLE popular_services DROP INDEX uk_popular_services_category_priority',
    'SELECT 1'
);
PREPARE stmt FROM @drop_uk_category_priority_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_active_service_category_key := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND column_name = 'active_service_category_key'
);
SET @add_active_service_category_key_sql := IF(
    @has_active_service_category_key = 0,
    'ALTER TABLE popular_services ADD COLUMN active_service_category_key BIGINT GENERATED ALWAYS AS (IF(is_active = 1, category_id, NULL)) STORED',
    'SELECT 1'
);
PREPARE stmt FROM @add_active_service_category_key_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_active_priority_key := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND column_name = 'active_priority_key'
);
SET @add_active_priority_key_sql := IF(
    @has_active_priority_key = 0,
    'ALTER TABLE popular_services ADD COLUMN active_priority_key INT GENERATED ALWAYS AS (IF(is_active = 1, priority, NULL)) STORED',
    'SELECT 1'
);
PREPARE stmt FROM @add_active_priority_key_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_active_service_category := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'uk_popular_services_active_service_category'
);
SET @create_uk_active_service_category_sql := IF(
    @has_uk_active_service_category = 0,
    'CREATE UNIQUE INDEX uk_popular_services_active_service_category ON popular_services(service_id, active_service_category_key)',
    'SELECT 1'
);
PREPARE stmt FROM @create_uk_active_service_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_active_category_priority := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'uk_popular_services_active_category_priority'
);
SET @create_uk_active_category_priority_sql := IF(
    @has_uk_active_category_priority = 0,
    'CREATE UNIQUE INDEX uk_popular_services_active_category_priority ON popular_services(category_id, active_priority_key)',
    'SELECT 1'
);
PREPARE stmt FROM @create_uk_active_category_priority_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
