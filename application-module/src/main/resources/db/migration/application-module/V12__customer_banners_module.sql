CREATE TABLE IF NOT EXISTS customer_banners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    redirect_type ENUM('CATEGORY', 'SERVICE', 'EXTERNAL') NOT NULL,
    redirect_id BIGINT NULL,
    redirect_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 100,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_banners_category FOREIGN KEY (category_id) REFERENCES admin_categories(id)
);

SET @idx_customer_banners_category_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_banners'
      AND index_name = 'idx_customer_banners_category_id'
);
SET @idx_customer_banners_category_sql := IF(
    @idx_customer_banners_category_exists = 0,
    'CREATE INDEX idx_customer_banners_category_id ON customer_banners(category_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_customer_banners_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_customer_banners_active_priority_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_banners'
      AND index_name = 'idx_customer_banners_active_priority'
);
SET @idx_customer_banners_active_priority_sql := IF(
    @idx_customer_banners_active_priority_exists = 0,
    'CREATE INDEX idx_customer_banners_active_priority ON customer_banners(is_active, priority)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_customer_banners_active_priority_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
