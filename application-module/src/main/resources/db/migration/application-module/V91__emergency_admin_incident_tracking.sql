ALTER TABLE emergency_event
    ADD COLUMN last_address VARCHAR(500) NULL AFTER last_accuracy_meters,
    ADD COLUMN acknowledgement_note VARCHAR(500) NULL AFTER last_address,
    ADD COLUMN resolved_note VARCHAR(500) NULL AFTER acknowledgement_note,
    ADD COLUMN acknowledged_at DATETIME(6) NULL AFTER resolved_note,
    ADD COLUMN acknowledged_by VARCHAR(120) NULL AFTER acknowledged_at,
    ADD COLUMN resolved_at DATETIME(6) NULL AFTER acknowledged_by,
    ADD COLUMN resolved_by VARCHAR(120) NULL AFTER resolved_at,
    ADD COLUMN request_id VARCHAR(80) NULL AFTER resolved_by,
    ADD COLUMN correlation_id VARCHAR(80) NULL AFTER request_id;
