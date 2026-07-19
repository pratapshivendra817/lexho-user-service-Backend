ALTER TABLE notifications
    MODIFY COLUMN notification_type VARCHAR(40) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE notification_templates
    MODIFY COLUMN notification_type VARCHAR(40) NOT NULL;
