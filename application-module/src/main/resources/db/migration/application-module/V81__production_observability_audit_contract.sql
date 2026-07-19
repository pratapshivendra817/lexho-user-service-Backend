ALTER TABLE payment_webhook_events
    ADD COLUMN idempotency_key VARCHAR(160) NULL,
    ADD COLUMN razorpay_payment_id VARCHAR(100) NULL;

UPDATE payment_webhook_events
SET idempotency_key = CONCAT('event:', event_id)
WHERE idempotency_key IS NULL;

ALTER TABLE payment_webhook_events
    MODIFY idempotency_key VARCHAR(160) NOT NULL;

CREATE UNIQUE INDEX uq_payment_webhook_events_idempotency_key
    ON payment_webhook_events(idempotency_key);

CREATE INDEX idx_payment_webhook_events_payment
    ON payment_webhook_events(razorpay_payment_id);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NULL,
    actor_role VARCHAR(40) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100) NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_entity_timestamp
    ON audit_log(entity_type, entity_id, timestamp);

CREATE INDEX idx_audit_log_actor_timestamp
    ON audit_log(actor_id, actor_role, timestamp);

CREATE INDEX idx_audit_log_action_timestamp
    ON audit_log(action, timestamp);
