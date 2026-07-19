CREATE TABLE IF NOT EXISTS payment_refunds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_transaction_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    requested_by_admin_id BIGINT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    razorpay_refund_id VARCHAR(100) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    refund_status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(500) NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    CONSTRAINT uq_payment_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_payment_refunds_razorpay_refund UNIQUE (razorpay_refund_id),
    CONSTRAINT fk_payment_refunds_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id),
    CONSTRAINT fk_payment_refunds_booking FOREIGN KEY (booking_id) REFERENCES orders(id)
);

CREATE INDEX idx_payment_refunds_booking_created
    ON payment_refunds(booking_id, created_at);

CREATE INDEX idx_payment_refunds_transaction_status
    ON payment_refunds(payment_transaction_id, refund_status);
