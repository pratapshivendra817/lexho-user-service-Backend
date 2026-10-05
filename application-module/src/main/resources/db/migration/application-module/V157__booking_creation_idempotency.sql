SET @schema_name := DATABASE();

CREATE TABLE IF NOT EXISTS booking_idempotency_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    booking_id BIGINT NULL,
    response_json TEXT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_booking_idempotency_customer_key (customer_id, idempotency_key),
    KEY idx_booking_idempotency_expires_at (expires_at),
    KEY idx_booking_idempotency_booking_id (booking_id)
);
