CREATE TABLE IF NOT EXISTS cities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    state VARCHAR(120) NULL,
    country VARCHAR(120) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'cities' AND index_name = 'uk_cities_name_state_country'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_cities_name_state_country ON cities(name, state, country)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'cities' AND index_name = 'idx_cities_active_deleted'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_cities_active_deleted ON cities(active, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS zones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    city_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    polygon_json LONGTEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @fk_exists := (
    SELECT COUNT(1) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'zones' AND constraint_name = 'fk_zones_city'
);
SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE zones ADD CONSTRAINT fk_zones_city FOREIGN KEY (city_id) REFERENCES cities(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'zones' AND index_name = 'uk_zones_city_name'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_zones_city_name ON zones(city_id, name)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'zones' AND index_name = 'idx_zones_city_active_deleted'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_zones_city_active_deleted ON zones(city_id, active, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'serviceable_areas' AND column_name = 'city_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE serviceable_areas ADD COLUMN city_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'serviceable_areas' AND column_name = 'zone_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE serviceable_areas ADD COLUMN zone_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'serviceable_areas' AND column_name = 'polygon_json'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE serviceable_areas ADD COLUMN polygon_json LONGTEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'serviceable_areas' AND index_name = 'idx_serviceable_areas_city_id'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_serviceable_areas_city_id ON serviceable_areas(city_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'serviceable_areas' AND index_name = 'idx_serviceable_areas_zone_id'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_serviceable_areas_zone_id ON serviceable_areas(zone_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(1) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'serviceable_areas' AND constraint_name = 'fk_serviceable_areas_city'
);
SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE serviceable_areas ADD CONSTRAINT fk_serviceable_areas_city FOREIGN KEY (city_id) REFERENCES cities(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(1) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'serviceable_areas' AND constraint_name = 'fk_serviceable_areas_zone'
);
SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE serviceable_areas ADD CONSTRAINT fk_serviceable_areas_zone FOREIGN KEY (zone_id) REFERENCES zones(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
