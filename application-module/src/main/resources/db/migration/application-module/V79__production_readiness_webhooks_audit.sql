ALTER TABLE payment_webhook_events
    ADD COLUMN booking_id BIGINT NULL,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN processed_at DATETIME NULL,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at DATETIME NULL,
    ADD COLUMN raw_payload TEXT NULL,
    ADD COLUMN failure_reason VARCHAR(500) NULL,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE INDEX idx_payment_webhook_events_status_retry
    ON payment_webhook_events(status, next_retry_at);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    partner_id BIGINT NULL,
    booking_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_booking_created
    ON audit_logs(booking_id, created_at);

CREATE INDEX idx_audit_logs_action_created
    ON audit_logs(action, created_at);
