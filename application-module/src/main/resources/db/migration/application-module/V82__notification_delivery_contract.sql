ALTER TABLE notifications
    ADD COLUMN delivered_at DATETIME NULL;

ALTER TABLE notification_logs
    ADD COLUMN provider_message_id VARCHAR(160) NULL,
    ADD COLUMN failure_code VARCHAR(80) NULL;
