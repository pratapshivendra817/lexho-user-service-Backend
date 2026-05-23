CREATE TABLE IF NOT EXISTS popular_services (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    priority INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @fk_popular_services_service_exists := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'fk_popular_services_service'
      AND constraint_type = 'FOREIGN KEY'
);
SET @fk_popular_services_service_sql := IF(
    @fk_popular_services_service_exists = 0,
    'ALTER TABLE popular_services ADD CONSTRAINT fk_popular_services_service FOREIGN KEY (service_id) REFERENCES services(id)',
    'SELECT 1'
);
PREPARE stmt FROM @fk_popular_services_service_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_popular_services_category_exists := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'fk_popular_services_category'
      AND constraint_type = 'FOREIGN KEY'
);
SET @fk_popular_services_category_sql := IF(
    @fk_popular_services_category_exists = 0,
    'ALTER TABLE popular_services ADD CONSTRAINT fk_popular_services_category FOREIGN KEY (category_id) REFERENCES admin_categories(id)',
    'SELECT 1'
);
PREPARE stmt FROM @fk_popular_services_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_popular_services_service_category_exists := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'uk_popular_services_service_category'
      AND constraint_type = 'UNIQUE'
);
SET @uk_popular_services_service_category_sql := IF(
    @uk_popular_services_service_category_exists = 0,
    'ALTER TABLE popular_services ADD CONSTRAINT uk_popular_services_service_category UNIQUE (service_id, category_id)',
    'SELECT 1'
);
PREPARE stmt FROM @uk_popular_services_service_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_popular_services_category_priority_exists := (
    SELECT COUNT(1)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND constraint_name = 'uk_popular_services_category_priority'
      AND constraint_type = 'UNIQUE'
);
SET @uk_popular_services_category_priority_sql := IF(
    @uk_popular_services_category_priority_exists = 0,
    'ALTER TABLE popular_services ADD CONSTRAINT uk_popular_services_category_priority UNIQUE (category_id, priority)',
    'SELECT 1'
);
PREPARE stmt FROM @uk_popular_services_category_priority_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_popular_services_category_active_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'popular_services'
      AND index_name = 'idx_popular_services_category_active_priority'
);
SET @idx_popular_services_category_active_sql := IF(
    @idx_popular_services_category_active_exists = 0,
    'CREATE INDEX idx_popular_services_category_active_priority ON popular_services(category_id, is_active, priority)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_popular_services_category_active_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
