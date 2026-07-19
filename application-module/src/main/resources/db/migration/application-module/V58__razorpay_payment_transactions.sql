CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    razorpay_signature VARCHAR(255) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    CONSTRAINT uq_payment_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_payment_transactions_razorpay_order UNIQUE (razorpay_order_id),
    CONSTRAINT uq_payment_transactions_razorpay_payment UNIQUE (razorpay_payment_id),
    CONSTRAINT fk_payment_transactions_booking FOREIGN KEY (booking_id) REFERENCES orders(id)
);

CREATE INDEX idx_payment_transactions_booking_created
    ON payment_transactions(booking_id, created_at);

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_webhook_events_event_id UNIQUE (event_id)
);
