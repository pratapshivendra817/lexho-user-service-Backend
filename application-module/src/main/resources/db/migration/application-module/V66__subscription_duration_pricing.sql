ALTER TABLE subscription_master
    ADD COLUMN per_visit_price DECIMAL(10,2) NULL AFTER duration_hours;

CREATE TABLE IF NOT EXISTS subscription_duration_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NULL,
    duration_hours DECIMAL(5,2) NOT NULL,
    per_visit_price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subscription_duration_price_service_active (service_id, is_active),
    INDEX idx_subscription_duration_price_duration (duration_hours)
);

INSERT INTO subscription_duration_price (service_id, duration_hours, per_visit_price, is_active)
SELECT NULL, 0.50, 99.00, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_duration_price
    WHERE service_id IS NULL AND duration_hours = 0.50
);

INSERT INTO subscription_duration_price (service_id, duration_hours, per_visit_price, is_active)
SELECT NULL, 1.00, 200.00, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_duration_price
    WHERE service_id IS NULL AND duration_hours = 1.00
);

INSERT INTO subscription_duration_price (service_id, duration_hours, per_visit_price, is_active)
SELECT NULL, 1.50, 250.00, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_duration_price
    WHERE service_id IS NULL AND duration_hours = 1.50
);

INSERT INTO subscription_duration_price (service_id, duration_hours, per_visit_price, is_active)
SELECT NULL, 2.00, 300.00, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_duration_price
    WHERE service_id IS NULL AND duration_hours = 2.00
);
