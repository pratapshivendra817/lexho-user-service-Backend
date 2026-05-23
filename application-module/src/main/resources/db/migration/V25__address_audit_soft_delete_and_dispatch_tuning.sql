SET @has_addr_is_deleted := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'is_deleted'
);
SET @sql_add_addr_is_deleted := IF(
    @has_addr_is_deleted = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_addr_is_deleted FROM @sql_add_addr_is_deleted;
EXECUTE stmt_add_addr_is_deleted;
DEALLOCATE PREPARE stmt_add_addr_is_deleted;

SET @has_addr_is_resolved := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'is_address_resolved'
);
SET @sql_add_addr_is_resolved := IF(
    @has_addr_is_resolved = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN is_address_resolved BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_addr_is_resolved FROM @sql_add_addr_is_resolved;
EXECUTE stmt_add_addr_is_resolved;
DEALLOCATE PREPARE stmt_add_addr_is_resolved;

SET @has_addr_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'created_at'
);
SET @sql_add_addr_created_at := IF(
    @has_addr_created_at = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_addr_created_at FROM @sql_add_addr_created_at;
EXECUTE stmt_add_addr_created_at;
DEALLOCATE PREPARE stmt_add_addr_created_at;

SET @has_addr_updated_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'updated_at'
);
SET @sql_add_addr_updated_at := IF(
    @has_addr_updated_at = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_addr_updated_at FROM @sql_add_addr_updated_at;
EXECUTE stmt_add_addr_updated_at;
DEALLOCATE PREPARE stmt_add_addr_updated_at;

SET @has_addr_created_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'created_by'
);
SET @sql_add_addr_created_by := IF(
    @has_addr_created_by = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN created_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_addr_created_by FROM @sql_add_addr_created_by;
EXECUTE stmt_add_addr_created_by;
DEALLOCATE PREPARE stmt_add_addr_created_by;

SET @has_addr_updated_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND column_name = 'updated_by'
);
SET @sql_add_addr_updated_by := IF(
    @has_addr_updated_by = 0,
    'ALTER TABLE lexho_addresses ADD COLUMN updated_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_addr_updated_by FROM @sql_add_addr_updated_by;
EXECUTE stmt_add_addr_updated_by;
DEALLOCATE PREPARE stmt_add_addr_updated_by;

SET @idx_addr_user_deleted_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_addresses'
      AND index_name = 'idx_lexho_addr_user_deleted'
);
SET @sql_idx_addr_user_deleted := IF(
    @idx_addr_user_deleted_exists = 0,
    'CREATE INDEX idx_lexho_addr_user_deleted ON lexho_addresses(user_id, is_deleted, is_default)',
    'SELECT 1'
);
PREPARE stmt_idx_addr_user_deleted FROM @sql_idx_addr_user_deleted;
EXECUTE stmt_idx_addr_user_deleted;
DEALLOCATE PREPARE stmt_idx_addr_user_deleted;

SET @idx_partner_location_geo_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND index_name = 'idx_partner_location_geo'
);
SET @sql_idx_partner_location_geo := IF(
    @idx_partner_location_geo_exists = 0,
    'CREATE INDEX idx_partner_location_geo ON current_partner_locations(latitude, longitude, updated_at)',
    'SELECT 1'
);
PREPARE stmt_idx_partner_location_geo FROM @sql_idx_partner_location_geo;
EXECUTE stmt_idx_partner_location_geo;
DEALLOCATE PREPARE stmt_idx_partner_location_geo;

