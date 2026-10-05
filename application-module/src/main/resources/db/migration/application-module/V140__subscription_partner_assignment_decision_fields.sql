ALTER TABLE subscription_partner_assignment
    ADD COLUMN IF NOT EXISTS decline_reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS accepted_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS declined_at DATETIME(6) NULL;
