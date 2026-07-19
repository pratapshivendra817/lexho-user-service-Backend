SET @has_orders_booking_type := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'booking_type'
);
SET @sql_orders_booking_type := IF(
    @has_orders_booking_type = 0,
    'ALTER TABLE orders ADD COLUMN booking_type VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt_orders_booking_type FROM @sql_orders_booking_type;
EXECUTE stmt_orders_booking_type;
DEALLOCATE PREPARE stmt_orders_booking_type;

SET @has_orders_promised_start_time := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'promised_start_time'
);
SET @sql_orders_promised_start_time := IF(
    @has_orders_promised_start_time = 0,
    'ALTER TABLE orders ADD COLUMN promised_start_time DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_orders_promised_start_time FROM @sql_orders_promised_start_time;
EXECUTE stmt_orders_promised_start_time;
DEALLOCATE PREPARE stmt_orders_promised_start_time;

SET @has_orders_sla_minutes := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'sla_minutes'
);
SET @sql_orders_sla_minutes := IF(
    @has_orders_sla_minutes = 0,
    'ALTER TABLE orders ADD COLUMN sla_minutes INT NULL',
    'SELECT 1'
);
PREPARE stmt_orders_sla_minutes FROM @sql_orders_sla_minutes;
EXECUTE stmt_orders_sla_minutes;
DEALLOCATE PREPARE stmt_orders_sla_minutes;

SET @has_orders_unavailable_reason := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'unavailable_reason'
);
SET @sql_orders_unavailable_reason := IF(
    @has_orders_unavailable_reason = 0,
    'ALTER TABLE orders ADD COLUMN unavailable_reason VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt_orders_unavailable_reason FROM @sql_orders_unavailable_reason;
EXECUTE stmt_orders_unavailable_reason;
DEALLOCATE PREPARE stmt_orders_unavailable_reason;

SET @has_psr_eta_minutes := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'partner_service_requests' AND column_name = 'eta_minutes'
);
SET @sql_psr_eta_minutes := IF(
    @has_psr_eta_minutes = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN eta_minutes INT NULL',
    'SELECT 1'
);
PREPARE stmt_psr_eta_minutes FROM @sql_psr_eta_minutes;
EXECUTE stmt_psr_eta_minutes;
DEALLOCATE PREPARE stmt_psr_eta_minutes;

SET @has_psr_expires_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'partner_service_requests' AND column_name = 'expires_at'
);
SET @sql_psr_expires_at := IF(
    @has_psr_expires_at = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN expires_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_psr_expires_at FROM @sql_psr_expires_at;
EXECUTE stmt_psr_expires_at;
DEALLOCATE PREPARE stmt_psr_expires_at;

