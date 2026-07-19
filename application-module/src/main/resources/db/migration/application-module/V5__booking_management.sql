CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NULL,
    service_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    booking_time TIME NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    notes VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_bookings_provider FOREIGN KEY (provider_id) REFERENCES customers(id),
    CONSTRAINT fk_bookings_service FOREIGN KEY (service_id) REFERENCES service_category(id)
);

CREATE INDEX idx_bookings_status ON bookings(booking_status);
CREATE INDEX idx_bookings_service_id ON bookings(service_id);
CREATE INDEX idx_bookings_date ON bookings(booking_date);

