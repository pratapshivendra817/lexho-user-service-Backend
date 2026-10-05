ALTER TABLE subscription_master
    ADD COLUMN idempotency_key VARCHAR(100) NULL,
    ADD COLUMN request_hash CHAR(64) NULL,
    ADD UNIQUE INDEX uq_subscription_customer_idempotency (customer_id, idempotency_key);

ALTER TABLE subscription_visit
    ADD UNIQUE INDEX uq_subscription_visit_schedule (subscription_id, visit_date);
