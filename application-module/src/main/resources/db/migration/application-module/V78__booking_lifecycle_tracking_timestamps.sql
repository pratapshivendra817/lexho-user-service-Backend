SET @has_orders_arrived_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'arrived_at'
);
SET @sql_orders_arrived_at := IF(
    @has_orders_arrived_at = 0,
    'ALTER TABLE orders ADD COLUMN arrived_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_orders_arrived_at FROM @sql_orders_arrived_at;
EXECUTE stmt_orders_arrived_at;
DEALLOCATE PREPARE stmt_orders_arrived_at;

SET @has_orders_started_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'started_at'
);
SET @sql_orders_started_at := IF(
    @has_orders_started_at = 0,
    'ALTER TABLE orders ADD COLUMN started_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_orders_started_at FROM @sql_orders_started_at;
EXECUTE stmt_orders_started_at;
DEALLOCATE PREPARE stmt_orders_started_at;

SET @has_orders_in_progress_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'in_progress_at'
);
SET @sql_orders_in_progress_at := IF(
    @has_orders_in_progress_at = 0,
    'ALTER TABLE orders ADD COLUMN in_progress_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_orders_in_progress_at FROM @sql_orders_in_progress_at;
EXECUTE stmt_orders_in_progress_at;
DEALLOCATE PREPARE stmt_orders_in_progress_at;

SET @has_orders_completed_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'completed_at'
);
SET @sql_orders_completed_at := IF(
    @has_orders_completed_at = 0,
    'ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_orders_completed_at FROM @sql_orders_completed_at;
EXECUTE stmt_orders_completed_at;
DEALLOCATE PREPARE stmt_orders_completed_at;
