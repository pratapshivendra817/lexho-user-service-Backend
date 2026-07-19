SET @has_orders_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'status'
);

SET @sql_orders_status := IF(
    @has_orders_status = 1,
    'ALTER TABLE orders MODIFY COLUMN status VARCHAR(50) NOT NULL',
    'SELECT 1'
);

PREPARE stmt_orders_status FROM @sql_orders_status;
EXECUTE stmt_orders_status;
DEALLOCATE PREPARE stmt_orders_status;
