SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'accepted_at') = 0,
    'ALTER TABLE orders ADD COLUMN accepted_at DATETIME(6) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'rejected_at') = 0,
    'ALTER TABLE orders ADD COLUMN rejected_at DATETIME(6) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_status_history' AND column_name = 'actor_type') = 0,
    'ALTER TABLE order_status_history ADD COLUMN actor_type VARCHAR(30) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_status_history' AND column_name = 'actor_id') = 0,
    'ALTER TABLE order_status_history ADD COLUMN actor_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'partners' AND column_name = 'kyc_workflow_status') = 0,
    'ALTER TABLE partners ADD COLUMN kyc_workflow_status VARCHAR(30) NOT NULL DEFAULT ''PENDING''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'partners' AND column_name = 'kyc_rejection_reason') = 0,
    'ALTER TABLE partners ADD COLUMN kyc_rejection_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'partners' AND column_name = 'bank_verification_status') = 0,
    'ALTER TABLE partners ADD COLUMN bank_verification_status VARCHAR(30) NOT NULL DEFAULT ''PENDING''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'partners' AND column_name = 'bank_rejection_reason') = 0,
    'ALTER TABLE partners ADD COLUMN bank_rejection_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS partner_device_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_partner_device_token UNIQUE (partner_id, token)
);

CREATE INDEX idx_partner_device_tokens_partner_active
    ON partner_device_tokens(partner_id, active);

CREATE TABLE IF NOT EXISTS partner_leaves (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_partner_leaves_partner_dates
    ON partner_leaves(partner_id, from_date, to_date);

CREATE TABLE IF NOT EXISTS partner_service_areas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    city VARCHAR(120) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_partner_service_area UNIQUE (partner_id, pincode)
);

CREATE INDEX idx_partner_service_areas_partner
    ON partner_service_areas(partner_id);

CREATE TABLE IF NOT EXISTS partner_penalties (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    created_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_partner_penalties_partner_created
    ON partner_penalties(partner_id, created_at);
