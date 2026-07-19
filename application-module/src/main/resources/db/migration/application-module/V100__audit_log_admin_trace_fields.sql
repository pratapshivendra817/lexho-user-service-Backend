ALTER TABLE audit_log
    ADD COLUMN actor_identifier VARCHAR(180) NULL AFTER actor_role,
    ADD COLUMN request_id VARCHAR(80) NULL AFTER new_value,
    ADD COLUMN correlation_id VARCHAR(80) NULL AFTER request_id;

CREATE INDEX idx_audit_log_actor_identifier_timestamp
    ON audit_log(actor_identifier, timestamp);
