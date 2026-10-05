SET @db_name = DATABASE();

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'arrived_at') = 0,
    'ALTER TABLE orders ADD COLUMN arrived_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_arrived_at_add FROM @stmt;
EXECUTE orders_arrived_at_add;
DEALLOCATE PREPARE orders_arrived_at_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'in_progress_at') = 0,
    'ALTER TABLE orders ADD COLUMN in_progress_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_in_progress_at_add FROM @stmt;
EXECUTE orders_in_progress_at_add;
DEALLOCATE PREPARE orders_in_progress_at_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_start_pin_nonce') = 0,
    'ALTER TABLE orders ADD COLUMN service_start_pin_nonce VARCHAR(64) NULL',
    'SELECT 1'
);
PREPARE orders_start_pin_nonce_add FROM @stmt;
EXECUTE orders_start_pin_nonce_add;
DEALLOCATE PREPARE orders_start_pin_nonce_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_start_pin_expires_at') = 0,
    'ALTER TABLE orders ADD COLUMN service_start_pin_expires_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_start_pin_expires_add FROM @stmt;
EXECUTE orders_start_pin_expires_add;
DEALLOCATE PREPARE orders_start_pin_expires_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_start_pin_attempts') = 0,
    'ALTER TABLE orders ADD COLUMN service_start_pin_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE orders_start_pin_attempts_add FROM @stmt;
EXECUTE orders_start_pin_attempts_add;
DEALLOCATE PREPARE orders_start_pin_attempts_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_start_pin_consumed_at') = 0,
    'ALTER TABLE orders ADD COLUMN service_start_pin_consumed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_start_pin_consumed_add FROM @stmt;
EXECUTE orders_start_pin_consumed_add;
DEALLOCATE PREPARE orders_start_pin_consumed_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_complete_pin_nonce') = 0,
    'ALTER TABLE orders ADD COLUMN service_complete_pin_nonce VARCHAR(64) NULL',
    'SELECT 1'
);
PREPARE orders_complete_pin_nonce_add FROM @stmt;
EXECUTE orders_complete_pin_nonce_add;
DEALLOCATE PREPARE orders_complete_pin_nonce_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_complete_pin_expires_at') = 0,
    'ALTER TABLE orders ADD COLUMN service_complete_pin_expires_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_complete_pin_expires_add FROM @stmt;
EXECUTE orders_complete_pin_expires_add;
DEALLOCATE PREPARE orders_complete_pin_expires_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_complete_pin_attempts') = 0,
    'ALTER TABLE orders ADD COLUMN service_complete_pin_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE orders_complete_pin_attempts_add FROM @stmt;
EXECUTE orders_complete_pin_attempts_add;
DEALLOCATE PREPARE orders_complete_pin_attempts_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'service_complete_pin_consumed_at') = 0,
    'ALTER TABLE orders ADD COLUMN service_complete_pin_consumed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE orders_complete_pin_consumed_add FROM @stmt;
EXECUTE orders_complete_pin_consumed_add;
DEALLOCATE PREPARE orders_complete_pin_consumed_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_service_start_pin_expires') = 0,
    'CREATE INDEX idx_orders_service_start_pin_expires ON orders(service_start_pin_expires_at)',
    'SELECT 1'
);
PREPARE orders_start_pin_expires_idx_add FROM @stmt;
EXECUTE orders_start_pin_expires_idx_add;
DEALLOCATE PREPARE orders_start_pin_expires_idx_add;

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_service_complete_pin_expires') = 0,
    'CREATE INDEX idx_orders_service_complete_pin_expires ON orders(service_complete_pin_expires_at)',
    'SELECT 1'
);
PREPARE orders_complete_pin_expires_idx_add FROM @stmt;
EXECUTE orders_complete_pin_expires_idx_add;
DEALLOCATE PREPARE orders_complete_pin_expires_idx_add;
