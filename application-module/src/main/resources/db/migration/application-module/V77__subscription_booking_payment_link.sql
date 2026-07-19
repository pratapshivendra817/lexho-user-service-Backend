SET @has_subscription_booking_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'subscription_master'
      AND column_name = 'booking_id'
);
SET @sql_subscription_booking_id := IF(
    @has_subscription_booking_id = 0,
    'ALTER TABLE subscription_master ADD COLUMN booking_id BIGINT NULL AFTER address_id',
    'SELECT 1'
);
PREPARE stmt_subscription_booking_id FROM @sql_subscription_booking_id;
EXECUTE stmt_subscription_booking_id;
DEALLOCATE PREPARE stmt_subscription_booking_id;

SET @has_subscription_booking_idx := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'subscription_master'
      AND index_name = 'idx_subscription_master_booking'
);
SET @sql_subscription_booking_idx := IF(
    @has_subscription_booking_idx = 0,
    'CREATE INDEX idx_subscription_master_booking ON subscription_master(booking_id)',
    'SELECT 1'
);
PREPARE stmt_subscription_booking_idx FROM @sql_subscription_booking_idx;
EXECUTE stmt_subscription_booking_idx;
DEALLOCATE PREPARE stmt_subscription_booking_idx;
