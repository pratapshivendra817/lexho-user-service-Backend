CREATE TABLE IF NOT EXISTS customer_addresses (
    id CHAR(36) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    custom_label VARCHAR(64),
    flat_no VARCHAR(32),
    floor VARCHAR(32),
    building_name VARCHAR(128),
    landmark VARCHAR(128),
    address_line VARCHAR(255) NOT NULL,
    area VARCHAR(128),
    sector VARCHAR(128),
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128) NOT NULL,
    country VARCHAR(128) NOT NULL DEFAULT 'India',
    pincode VARCHAR(16) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    mobile VARCHAR(20),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    updated_by VARCHAR(128)
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses(customer_id);
CREATE INDEX idx_customer_addresses_pincode ON customer_addresses(pincode);
CREATE INDEX idx_customer_addresses_is_default ON customer_addresses(is_default);

CREATE TABLE IF NOT EXISTS customer_current_locations (
    id CHAR(36) PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    formatted_address VARCHAR(255),
    city VARCHAR(128),
    state VARCHAR(128),
    pincode VARCHAR(16),
    service_available BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS serviceable_pincodes (
    id CHAR(36) PRIMARY KEY,
    pincode VARCHAR(16) NOT NULL UNIQUE,
    city VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT IGNORE INTO serviceable_pincodes (id, pincode, city, active) VALUES
(UUID(), '201304', 'Noida', TRUE),
(UUID(), '110001', 'New Delhi', TRUE),
(UUID(), '122001', 'Gurugram', TRUE);
