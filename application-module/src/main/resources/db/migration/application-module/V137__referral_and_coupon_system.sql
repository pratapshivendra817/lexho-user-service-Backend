CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12, 2) NOT NULL,
    max_discount_amount DECIMAL(12, 2) NULL,
    minimum_purchase_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_usage_limit INT NULL,
    per_user_usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    valid_from DATETIME NULL,
    valid_until DATETIME NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    applicable_service_id BIGINT NULL,
    applicable_booking_type VARCHAR(30) NULL,
    assigned_user_id BIGINT NULL,
    source_type VARCHAR(40) NULL,
    source_reference_id VARCHAR(80) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_coupons_code UNIQUE (code)
);

CREATE INDEX idx_coupons_active_validity ON coupons(active, valid_from, valid_until);
CREATE INDEX idx_coupons_service_booking_type ON coupons(applicable_service_id, applicable_booking_type);
CREATE UNIQUE INDEX uq_coupons_source_reference ON coupons(source_type, source_reference_id);
CREATE INDEX idx_coupons_assigned_user ON coupons(assigned_user_id, active);

CREATE TABLE IF NOT EXISTS coupon_redemptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    redeemed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_coupon_redemptions_booking UNIQUE (booking_id),
    CONSTRAINT uq_coupon_redemptions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_coupon_redemptions_booking FOREIGN KEY (booking_id) REFERENCES orders(id)
);

CREATE INDEX idx_coupon_redemptions_coupon_user ON coupon_redemptions(coupon_id, user_id);
CREATE INDEX idx_coupon_redemptions_coupon ON coupon_redemptions(coupon_id);

CREATE TABLE IF NOT EXISTS referral_config (
    id BIGINT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    referrer_reward_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    referred_user_reward_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    minimum_qualifying_purchase_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    qualification_condition VARCHAR(40) NOT NULL DEFAULT 'SUCCESSFUL_PAYMENT',
    expiry_days INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT NULL
);

INSERT INTO referral_config (
    id,
    active,
    referrer_reward_amount,
    referred_user_reward_amount,
    minimum_qualifying_purchase_amount,
    qualification_condition
) VALUES (
    1,
    FALSE,
    0.00,
    0.00,
    0.00,
    'SUCCESSFUL_PAYMENT'
) ON DUPLICATE KEY UPDATE id = id;

SET @has_referrer_user_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'referrer_user_id'
);
SET @sql_referrer_user_id := IF(
    @has_referrer_user_id = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN referrer_user_id BIGINT NULL AFTER code',
    'SELECT 1'
);
PREPARE stmt_referrer_user_id FROM @sql_referrer_user_id;
EXECUTE stmt_referrer_user_id;
DEALLOCATE PREPARE stmt_referrer_user_id;

SET @has_referral_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'status'
);
SET @sql_referral_status := IF(
    @has_referral_status = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' AFTER referrer_user_id',
    'SELECT 1'
);
PREPARE stmt_referral_status FROM @sql_referral_status;
EXECUTE stmt_referral_status;
DEALLOCATE PREPARE stmt_referral_status;

SET @has_referral_qualified_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'qualified_at'
);
SET @sql_referral_qualified_at := IF(
    @has_referral_qualified_at = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN qualified_at DATETIME NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt_referral_qualified_at FROM @sql_referral_qualified_at;
EXECUTE stmt_referral_qualified_at;
DEALLOCATE PREPARE stmt_referral_qualified_at;

SET @has_referral_rewarded_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'rewarded_at'
);
SET @sql_referral_rewarded_at := IF(
    @has_referral_rewarded_at = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN rewarded_at DATETIME NULL AFTER qualified_at',
    'SELECT 1'
);
PREPARE stmt_referral_rewarded_at FROM @sql_referral_rewarded_at;
EXECUTE stmt_referral_rewarded_at;
DEALLOCATE PREPARE stmt_referral_rewarded_at;

SET @has_referral_expires_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'expires_at'
);
SET @sql_referral_expires_at := IF(
    @has_referral_expires_at = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN expires_at DATETIME NULL AFTER rewarded_at',
    'SELECT 1'
);
PREPARE stmt_referral_expires_at FROM @sql_referral_expires_at;
EXECUTE stmt_referral_expires_at;
DEALLOCATE PREPARE stmt_referral_expires_at;

SET @has_referral_booking_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND column_name = 'qualifying_booking_id'
);
SET @sql_referral_booking_id := IF(
    @has_referral_booking_id = 0,
    'ALTER TABLE lexho_referral_uses ADD COLUMN qualifying_booking_id BIGINT NULL AFTER expires_at',
    'SELECT 1'
);
PREPARE stmt_referral_booking_id FROM @sql_referral_booking_id;
EXECUTE stmt_referral_booking_id;
DEALLOCATE PREPARE stmt_referral_booking_id;

CREATE TABLE IF NOT EXISTS referral_reward_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    referral_use_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    beneficiary VARCHAR(30) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    qualifying_booking_id BIGINT NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    reward_coupon_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_referral_rewards_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_referral_rewards_referral_use FOREIGN KEY (referral_use_id) REFERENCES lexho_referral_uses(id),
    CONSTRAINT fk_referral_rewards_coupon FOREIGN KEY (reward_coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_referral_rewards_booking FOREIGN KEY (qualifying_booking_id) REFERENCES orders(id)
);

CREATE INDEX idx_referral_rewards_user ON referral_reward_transactions(user_id, created_at);
CREATE INDEX idx_referral_rewards_referral ON referral_reward_transactions(referral_use_id);

SET @has_orders_original_amount := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'original_amount'
);
SET @sql_orders_original_amount := IF(
    @has_orders_original_amount = 0,
    'ALTER TABLE orders ADD COLUMN original_amount DECIMAL(12, 2) NULL AFTER total_amount',
    'SELECT 1'
);
PREPARE stmt_orders_original_amount FROM @sql_orders_original_amount;
EXECUTE stmt_orders_original_amount;
DEALLOCATE PREPARE stmt_orders_original_amount;

SET @has_orders_discount_amount := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'discount_amount'
);
SET @sql_orders_discount_amount := IF(
    @has_orders_discount_amount = 0,
    'ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(12, 2) NULL AFTER original_amount',
    'SELECT 1'
);
PREPARE stmt_orders_discount_amount FROM @sql_orders_discount_amount;
EXECUTE stmt_orders_discount_amount;
DEALLOCATE PREPARE stmt_orders_discount_amount;

SET @has_orders_coupon_code := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'coupon_code'
);
SET @sql_orders_coupon_code := IF(
    @has_orders_coupon_code = 0,
    'ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(40) NULL AFTER discount_amount',
    'SELECT 1'
);
PREPARE stmt_orders_coupon_code FROM @sql_orders_coupon_code;
EXECUTE stmt_orders_coupon_code;
DEALLOCATE PREPARE stmt_orders_coupon_code;

SET @has_referral_use_user_unique := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_uses'
      AND index_name = 'uq_lexho_referral_uses_user'
);
SET @sql_referral_use_user_unique := IF(
    @has_referral_use_user_unique = 0,
    'CREATE UNIQUE INDEX uq_lexho_referral_uses_user ON lexho_referral_uses(user_id)',
    'SELECT 1'
);
PREPARE stmt_referral_use_user_unique FROM @sql_referral_use_user_unique;
EXECUTE stmt_referral_use_user_unique;
DEALLOCATE PREPARE stmt_referral_use_user_unique;

SET @has_referral_code_user_unique := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_referral_codes'
      AND index_name = 'uq_lexho_referral_codes_user'
);
SET @sql_referral_code_user_unique := IF(
    @has_referral_code_user_unique = 0,
    'CREATE UNIQUE INDEX uq_lexho_referral_codes_user ON lexho_referral_codes(user_id)',
    'SELECT 1'
);
PREPARE stmt_referral_code_user_unique FROM @sql_referral_code_user_unique;
EXECUTE stmt_referral_code_user_unique;
DEALLOCATE PREPARE stmt_referral_code_user_unique;

SET @has_orders_coupon_code_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND index_name = 'idx_orders_coupon_code'
);
SET @sql_orders_coupon_code_index := IF(
    @has_orders_coupon_code_index = 0,
    'CREATE INDEX idx_orders_coupon_code ON orders(coupon_code)',
    'SELECT 1'
);
PREPARE stmt_orders_coupon_code_index FROM @sql_orders_coupon_code_index;
EXECUTE stmt_orders_coupon_code_index;
DEALLOCATE PREPARE stmt_orders_coupon_code_index;
