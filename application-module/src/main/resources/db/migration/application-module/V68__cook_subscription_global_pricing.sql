ALTER TABLE cook_subscription_pricing
    MODIFY COLUMN service_id BIGINT NULL;

SET @drop_cook_pricing_unique_key_sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cook_subscription_pricing'
          AND index_name = 'uk_cook_subscription_pricing_service_family'
    ),
    'ALTER TABLE cook_subscription_pricing DROP INDEX uk_cook_subscription_pricing_service_family',
    'SELECT 1'
);
PREPARE stmt_drop_cook_pricing_unique_key FROM @drop_cook_pricing_unique_key_sql;
EXECUTE stmt_drop_cook_pricing_unique_key;
DEALLOCATE PREPARE stmt_drop_cook_pricing_unique_key;
