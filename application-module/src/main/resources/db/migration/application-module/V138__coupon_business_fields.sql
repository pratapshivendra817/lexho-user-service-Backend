SET @has_coupon_title := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'title'
);
SET @sql_coupon_title := IF(
    @has_coupon_title = 0,
    'ALTER TABLE coupons ADD COLUMN title VARCHAR(120) NULL AFTER code',
    'SELECT 1'
);
PREPARE stmt_coupon_title FROM @sql_coupon_title;
EXECUTE stmt_coupon_title;
DEALLOCATE PREPARE stmt_coupon_title;

SET @has_coupon_description := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'description'
);
SET @sql_coupon_description := IF(
    @has_coupon_description = 0,
    'ALTER TABLE coupons ADD COLUMN description VARCHAR(1000) NULL AFTER title',
    'SELECT 1'
);
PREPARE stmt_coupon_description FROM @sql_coupon_description;
EXECUTE stmt_coupon_description;
DEALLOCATE PREPARE stmt_coupon_description;

SET @has_coupon_customer_visible := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'customer_visible'
);
SET @sql_coupon_customer_visible := IF(
    @has_coupon_customer_visible = 0,
    'ALTER TABLE coupons ADD COLUMN customer_visible BOOLEAN NOT NULL DEFAULT TRUE AFTER active',
    'SELECT 1'
);
PREPARE stmt_coupon_customer_visible FROM @sql_coupon_customer_visible;
EXECUTE stmt_coupon_customer_visible;
DEALLOCATE PREPARE stmt_coupon_customer_visible;

SET @has_coupon_first_booking_only := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'first_booking_only'
);
SET @sql_coupon_first_booking_only := IF(
    @has_coupon_first_booking_only = 0,
    'ALTER TABLE coupons ADD COLUMN first_booking_only BOOLEAN NOT NULL DEFAULT FALSE AFTER customer_visible',
    'SELECT 1'
);
PREPARE stmt_coupon_first_booking_only FROM @sql_coupon_first_booking_only;
EXECUTE stmt_coupon_first_booking_only;
DEALLOCATE PREPARE stmt_coupon_first_booking_only;

SET @has_coupon_applicable_category_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'applicable_category_id'
);
SET @sql_coupon_applicable_category_id := IF(
    @has_coupon_applicable_category_id = 0,
    'ALTER TABLE coupons ADD COLUMN applicable_category_id BIGINT NULL AFTER applicable_service_id',
    'SELECT 1'
);
PREPARE stmt_coupon_applicable_category_id FROM @sql_coupon_applicable_category_id;
EXECUTE stmt_coupon_applicable_category_id;
DEALLOCATE PREPARE stmt_coupon_applicable_category_id;

SET @has_coupon_max_uses_per_day := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND column_name = 'max_uses_per_day'
);
SET @sql_coupon_max_uses_per_day := IF(
    @has_coupon_max_uses_per_day = 0,
    'ALTER TABLE coupons ADD COLUMN max_uses_per_day INT NULL AFTER applicable_booking_type',
    'SELECT 1'
);
PREPARE stmt_coupon_max_uses_per_day FROM @sql_coupon_max_uses_per_day;
EXECUTE stmt_coupon_max_uses_per_day;
DEALLOCATE PREPARE stmt_coupon_max_uses_per_day;

SET @has_idx_coupons_category := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'coupons'
      AND index_name = 'idx_coupons_category'
);
SET @sql_idx_coupons_category := IF(
    @has_idx_coupons_category = 0,
    'CREATE INDEX idx_coupons_category ON coupons(applicable_category_id)',
    'SELECT 1'
);
PREPARE stmt_idx_coupons_category FROM @sql_idx_coupons_category;
EXECUTE stmt_idx_coupons_category;
DEALLOCATE PREPARE stmt_idx_coupons_category;
