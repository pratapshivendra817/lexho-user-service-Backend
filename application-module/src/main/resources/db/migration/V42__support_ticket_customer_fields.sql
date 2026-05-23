ALTER TABLE support_tickets
    ADD COLUMN category_id VARCHAR(100) NULL AFTER status,
    ADD COLUMN booking_id VARCHAR(100) NULL AFTER category_id,
    ADD COLUMN priority VARCHAR(20) NULL AFTER booking_id;

