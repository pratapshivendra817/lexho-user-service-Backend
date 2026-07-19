SET @has_ps_deleted := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
      AND column_name = 'deleted'
);
SET @sql_add_ps_deleted := IF(
    @has_ps_deleted = 0,
    'ALTER TABLE partner_shifts ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_ps_deleted FROM @sql_add_ps_deleted;
EXECUTE stmt_add_ps_deleted;
DEALLOCATE PREPARE stmt_add_ps_deleted;

SET @has_ps_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
      AND column_name = 'created_at'
);
SET @sql_add_ps_created_at := IF(
    @has_ps_created_at = 0,
    'ALTER TABLE partner_shifts ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_ps_created_at FROM @sql_add_ps_created_at;
EXECUTE stmt_add_ps_created_at;
DEALLOCATE PREPARE stmt_add_ps_created_at;

SET @has_ps_created_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
      AND column_name = 'created_by'
);
SET @sql_add_ps_created_by := IF(
    @has_ps_created_by = 0,
    'ALTER TABLE partner_shifts ADD COLUMN created_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_ps_created_by FROM @sql_add_ps_created_by;
EXECUTE stmt_add_ps_created_by;
DEALLOCATE PREPARE stmt_add_ps_created_by;

SET @has_ps_updated_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
      AND column_name = 'updated_by'
);
SET @sql_add_ps_updated_by := IF(
    @has_ps_updated_by = 0,
    'ALTER TABLE partner_shifts ADD COLUMN updated_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_ps_updated_by FROM @sql_add_ps_updated_by;
EXECUTE stmt_add_ps_updated_by;
DEALLOCATE PREPARE stmt_add_ps_updated_by;

SET @has_cpl_deleted := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND column_name = 'deleted'
);
SET @sql_add_cpl_deleted := IF(
    @has_cpl_deleted = 0,
    'ALTER TABLE current_partner_locations ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_cpl_deleted FROM @sql_add_cpl_deleted;
EXECUTE stmt_add_cpl_deleted;
DEALLOCATE PREPARE stmt_add_cpl_deleted;

SET @has_cpl_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND column_name = 'created_at'
);
SET @sql_add_cpl_created_at := IF(
    @has_cpl_created_at = 0,
    'ALTER TABLE current_partner_locations ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_cpl_created_at FROM @sql_add_cpl_created_at;
EXECUTE stmt_add_cpl_created_at;
DEALLOCATE PREPARE stmt_add_cpl_created_at;

SET @has_cpl_created_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND column_name = 'created_by'
);
SET @sql_add_cpl_created_by := IF(
    @has_cpl_created_by = 0,
    'ALTER TABLE current_partner_locations ADD COLUMN created_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_cpl_created_by FROM @sql_add_cpl_created_by;
EXECUTE stmt_add_cpl_created_by;
DEALLOCATE PREPARE stmt_add_cpl_created_by;

SET @has_cpl_updated_by := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND column_name = 'updated_by'
);
SET @sql_add_cpl_updated_by := IF(
    @has_cpl_updated_by = 0,
    'ALTER TABLE current_partner_locations ADD COLUMN updated_by VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_cpl_updated_by FROM @sql_add_cpl_updated_by;
EXECUTE stmt_add_cpl_updated_by;
DEALLOCATE PREPARE stmt_add_cpl_updated_by;

SET @idx_ps_active_lookup_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
      AND index_name = 'idx_partner_shifts_active_lookup'
);
SET @sql_idx_ps_active_lookup := IF(
    @idx_ps_active_lookup_exists = 0,
    'CREATE INDEX idx_partner_shifts_active_lookup ON partner_shifts(partner_id, deleted, is_online)',
    'SELECT 1'
);
PREPARE stmt_idx_ps_active_lookup FROM @sql_idx_ps_active_lookup;
EXECUTE stmt_idx_ps_active_lookup;
DEALLOCATE PREPARE stmt_idx_ps_active_lookup;

SET @idx_cpl_active_lookup_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND index_name = 'idx_current_partner_locations_active_lookup'
);
SET @sql_idx_cpl_active_lookup := IF(
    @idx_cpl_active_lookup_exists = 0,
    'CREATE INDEX idx_current_partner_locations_active_lookup ON current_partner_locations(partner_id, deleted, updated_at)',
    'SELECT 1'
);
PREPARE stmt_idx_cpl_active_lookup FROM @sql_idx_cpl_active_lookup;
EXECUTE stmt_idx_cpl_active_lookup;
DEALLOCATE PREPARE stmt_idx_cpl_active_lookup;

