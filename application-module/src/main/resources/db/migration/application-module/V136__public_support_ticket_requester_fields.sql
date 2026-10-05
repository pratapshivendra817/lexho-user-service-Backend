ALTER TABLE support_tickets
    ADD COLUMN requester_name VARCHAR(120) NULL AFTER booking_id,
    ADD COLUMN requester_email VARCHAR(190) NULL AFTER requester_name,
    ADD COLUMN requester_phone VARCHAR(30) NULL AFTER requester_email;
