SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partner_service_requests'
              AND column_name = 'subscription_id'
        ),
        'SELECT 1',
        'ALTER TABLE partner_service_requests ADD COLUMN subscription_id BIGINT NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partner_service_requests'
              AND column_name = 'subscription_visit_id'
        ),
        'SELECT 1',
        'ALTER TABLE partner_service_requests ADD COLUMN subscription_visit_id BIGINT NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_psr_subscription_visit_status ON partner_service_requests(subscription_visit_id, status);
