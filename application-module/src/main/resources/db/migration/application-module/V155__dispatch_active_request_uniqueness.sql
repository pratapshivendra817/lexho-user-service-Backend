SET @schema_name := DATABASE();

SET @has_active_visit_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'partner_service_requests'
      AND column_name = 'active_subscription_visit_id'
);
SET @sql := IF(@has_active_visit_column = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN active_subscription_visit_id BIGINT GENERATED ALWAYS AS (CASE WHEN subscription_visit_id IS NOT NULL AND status IN (''PENDING'', ''ACCEPTED'') THEN subscription_visit_id ELSE NULL END) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_active_visit_index := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'partner_service_requests'
      AND index_name = 'uq_psr_active_subscription_visit'
);
SET @sql := IF(@has_active_visit_index = 0,
    'CREATE UNIQUE INDEX uq_psr_active_subscription_visit ON partner_service_requests (active_subscription_visit_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_active_order_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'partner_service_requests'
      AND column_name = 'active_order_id'
);
SET @sql := IF(@has_active_order_column = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN active_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN subscription_visit_id IS NULL AND status = ''PENDING'' THEN cart_id ELSE NULL END) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_active_order_index := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'partner_service_requests'
      AND index_name = 'uq_psr_active_order_partner'
);
SET @sql := IF(@has_active_order_index = 0,
    'CREATE UNIQUE INDEX uq_psr_active_order_partner ON partner_service_requests (active_order_id, partner_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
