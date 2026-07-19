ALTER TABLE cook_subscription_pricing
    MODIFY COLUMN service_id BIGINT NULL;

DROP PROCEDURE IF EXISTS drop_cook_pricing_unique_key;

DELIMITER //
CREATE PROCEDURE drop_cook_pricing_unique_key()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cook_subscription_pricing'
          AND index_name = 'uk_cook_subscription_pricing_service_family'
    ) THEN
        ALTER TABLE cook_subscription_pricing
            DROP INDEX uk_cook_subscription_pricing_service_family;
    END IF;
END //
DELIMITER ;

CALL drop_cook_pricing_unique_key();
DROP PROCEDURE IF EXISTS drop_cook_pricing_unique_key;
