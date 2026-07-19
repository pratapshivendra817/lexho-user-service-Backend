SET @db_name = DATABASE();

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'gross_amount') = 0,
        'ALTER TABLE payouts ADD COLUMN gross_amount DECIMAL(12,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'commission_percent') = 0,
        'ALTER TABLE payouts ADD COLUMN commission_percent DECIMAL(5,2) NOT NULL DEFAULT 15.00',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'commission_amount') = 0,
        'ALTER TABLE payouts ADD COLUMN commission_amount DECIMAL(12,2) NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'currency') = 0,
        'ALTER TABLE payouts ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT ''INR''',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'reversed_amount') = 0,
        'ALTER TABLE payouts ADD COLUMN reversed_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payouts' AND COLUMN_NAME = 'reversed_at') = 0,
        'ALTER TABLE payouts ADD COLUMN reversed_at DATETIME NULL',
        'SELECT 1'
           );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS partner_settlement_refunds (
    razorpay_refund_id VARCHAR(100) PRIMARY KEY,
    payout_id BIGINT NOT NULL,
    reversed_amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_partner_settlement_refund_payout FOREIGN KEY (payout_id) REFERENCES payouts(id)
);

CREATE TABLE IF NOT EXISTS partner_withdrawal_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    INDEX idx_partner_withdrawals_partner_created (partner_id, created_at),
    INDEX idx_partner_withdrawals_status (status)
);
