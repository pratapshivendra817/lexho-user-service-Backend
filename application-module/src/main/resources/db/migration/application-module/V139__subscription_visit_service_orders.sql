ALTER TABLE subscription_visit ADD COLUMN booking_id BIGINT NULL;
CREATE UNIQUE INDEX uk_subscription_visit_booking ON subscription_visit (booking_id);
ALTER TABLE subscription_visit ADD CONSTRAINT fk_subscription_visit_booking FOREIGN KEY (booking_id) REFERENCES orders (id);
