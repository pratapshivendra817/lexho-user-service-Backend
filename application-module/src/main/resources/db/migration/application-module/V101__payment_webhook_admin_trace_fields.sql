ALTER TABLE payment_webhook_events
    ADD COLUMN request_id VARCHAR(80) NULL AFTER failure_reason,
    ADD COLUMN correlation_id VARCHAR(80) NULL AFTER request_id;

CREATE INDEX idx_payment_webhook_events_event_type_status
    ON payment_webhook_events(event_type, status);
