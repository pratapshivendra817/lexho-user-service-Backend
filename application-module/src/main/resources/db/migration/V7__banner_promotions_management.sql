CREATE TABLE IF NOT EXISTS banner_promotions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255) NULL,
    description VARCHAR(1000) NULL,
    banner_image_url VARCHAR(1000) NOT NULL,
    promo_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    slot_number INT NOT NULL,
    priority INT NOT NULL,
    discount_percentage DOUBLE NULL,
    coupon_code VARCHAR(80) NULL UNIQUE,
    coupon_id BIGINT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    redirect_type VARCHAR(20) NOT NULL,
    redirect_value VARCHAR(1000) NULL,
    city VARCHAR(100) NULL,
    service_category_id BIGINT NULL,
    new_user_only BOOLEAN NOT NULL DEFAULT FALSE,
    premium_user_only BOOLEAN NOT NULL DEFAULT FALSE,
    min_order_value DECIMAL(12, 2) NULL,
    platform_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    click_count BIGINT NOT NULL DEFAULT 0,
    impression_count BIGINT NOT NULL DEFAULT 0,
    redemption_count BIGINT NOT NULL DEFAULT 0,
    revenue_generated DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    max_views_per_user_per_day INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_banner_promo_status ON banner_promotions(status);
CREATE INDEX idx_banner_promo_city ON banner_promotions(city);
CREATE INDEX idx_banner_promo_slot ON banner_promotions(slot_number);
CREATE INDEX idx_banner_promo_start_date ON banner_promotions(start_date);
CREATE INDEX idx_banner_promo_end_date ON banner_promotions(end_date);

CREATE TABLE IF NOT EXISTS banner_impression_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    banner_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    device_id VARCHAR(120) NULL,
    city VARCHAR(100) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS banner_click_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    banner_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    device_id VARCHAR(120) NULL,
    city VARCHAR(100) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS promo_redemption_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    banner_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    booking_id BIGINT NULL,
    discount_applied DECIMAL(12, 2) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

