ALTER TABLE payouts
    ADD CONSTRAINT uq_payouts_reference_booking UNIQUE (reference_booking_id);
