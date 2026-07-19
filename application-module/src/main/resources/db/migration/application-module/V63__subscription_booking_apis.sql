CREATE TABLE IF NOT EXISTS subscription_master (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    visits INT NOT NULL,
    duration_hours DECIMAL(5,2) NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subscription_master_customer (customer_id),
    INDEX idx_subscription_master_status (status),
    INDEX idx_subscription_master_service (service_id)
);

CREATE TABLE IF NOT EXISTS subscription_frequency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    day_name VARCHAR(20) NOT NULL,
    INDEX idx_subscription_frequency_subscription (subscription_id),
    CONSTRAINT fk_subscription_frequency_master
        FOREIGN KEY (subscription_id) REFERENCES subscription_master(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscription_visit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    rescheduled_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_subscription_visit_subscription (subscription_id),
    INDEX idx_subscription_visit_date (subscription_id, visit_date),
    CONSTRAINT fk_subscription_visit_master
        FOREIGN KEY (subscription_id) REFERENCES subscription_master(id)
        ON DELETE CASCADE
);
