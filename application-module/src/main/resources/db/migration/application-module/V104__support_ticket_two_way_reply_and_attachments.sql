ALTER TABLE support_tickets
    ADD COLUMN attachment_urls TEXT NULL AFTER priority;

ALTER TABLE support_ticket_replies
    MODIFY COLUMN admin_id BIGINT NULL,
    ADD COLUMN sender_type VARCHAR(20) NOT NULL DEFAULT 'SUPPORT' AFTER admin_id,
    ADD COLUMN attachment_urls TEXT NULL AFTER message;
