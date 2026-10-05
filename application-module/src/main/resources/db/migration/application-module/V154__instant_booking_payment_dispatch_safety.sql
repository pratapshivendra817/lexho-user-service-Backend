SET @schema_name := DATABASE();

SET @has_dispatch_address_id := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'dispatch_address_id'
);
SET @sql := IF(@has_dispatch_address_id = 0,
    'ALTER TABLE orders ADD COLUMN dispatch_address_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_dispatch_service_id := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'dispatch_service_id'
);
SET @sql := IF(@has_dispatch_service_id = 0,
    'ALTER TABLE orders ADD COLUMN dispatch_service_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_dispatch_quantity := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'dispatch_quantity'
);
SET @sql := IF(@has_dispatch_quantity = 0,
    'ALTER TABLE orders ADD COLUMN dispatch_quantity INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_dispatch_started_at := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'dispatch_started_at'
);
SET @sql := IF(@has_dispatch_started_at = 0,
    'ALTER TABLE orders ADD COLUMN dispatch_started_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_orders_timeout_idx := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'orders' AND index_name = 'idx_orders_instant_search_timeout'
);
SET @sql := IF(@has_orders_timeout_idx = 0,
    'CREATE INDEX idx_orders_instant_search_timeout ON orders (booking_type, status, assigned_partner_id, created_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_accepted_cart_id := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'partner_service_requests' AND column_name = 'accepted_cart_id'
);
SET @sql := IF(@has_accepted_cart_id = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN accepted_cart_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = ''ACCEPTED'' AND subscription_visit_id IS NULL THEN cart_id ELSE NULL END) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_unique_accepted_order := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'partner_service_requests' AND index_name = 'uq_partner_request_one_accepted_order'
);
SET @sql := IF(@has_unique_accepted_order = 0,
    'CREATE UNIQUE INDEX uq_partner_request_one_accepted_order ON partner_service_requests (accepted_cart_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
