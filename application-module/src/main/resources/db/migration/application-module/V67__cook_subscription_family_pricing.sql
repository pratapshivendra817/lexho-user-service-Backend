ALTER TABLE subscription_master
    MODIFY COLUMN duration_hours DECIMAL(5,2) NULL,
    ADD COLUMN booking_mode VARCHAR(30) NULL AFTER address_id,
    ADD COLUMN service_type VARCHAR(30) NULL AFTER booking_mode,
    ADD COLUMN family_size VARCHAR(20) NULL AFTER duration_hours;

CREATE TABLE IF NOT EXISTS cook_subscription_pricing (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    family_size VARCHAR(20) NOT NULL,
    price_per_visit DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cook_subscription_pricing_service_family (service_id, family_size),
    INDEX idx_cook_subscription_pricing_service_active (service_id, is_active)
);
