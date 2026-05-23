SET @col_discounted_price_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'services'
      AND column_name = 'discounted_price'
);
SET @col_discounted_price_sql := IF(
    @col_discounted_price_exists = 0,
    'ALTER TABLE services ADD COLUMN discounted_price DECIMAL(12, 2) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @col_discounted_price_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_total_reviews_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'services'
      AND column_name = 'total_reviews'
);
SET @col_total_reviews_sql := IF(
    @col_total_reviews_exists = 0,
    'ALTER TABLE services ADD COLUMN total_reviews BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @col_total_reviews_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_is_new_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'services'
      AND column_name = 'is_new'
);
SET @col_is_new_sql := IF(
    @col_is_new_exists = 0,
    'ALTER TABLE services ADD COLUMN is_new BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt FROM @col_is_new_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_services_category_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'services'
      AND index_name = 'idx_services_category_id'
);
SET @idx_services_category_id_sql := IF(
    @idx_services_category_id_exists = 0,
    'CREATE INDEX idx_services_category_id ON services(category_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_services_category_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_cart_items_user_service UNIQUE (user_id, service_id),
    CONSTRAINT fk_cart_items_service FOREIGN KEY (service_id) REFERENCES services(id)
);

SET @idx_cart_items_user_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND index_name = 'idx_cart_items_user_id'
);
SET @idx_cart_items_user_id_sql := IF(
    @idx_cart_items_user_id_exists = 0,
    'CREATE INDEX idx_cart_items_user_id ON cart_items(user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_cart_items_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_cart_items_service_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cart_items'
      AND index_name = 'idx_cart_items_service_id'
);
SET @idx_cart_items_service_id_sql := IF(
    @idx_cart_items_service_id_exists = 0,
    'CREATE INDEX idx_cart_items_service_id ON cart_items(service_id)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_cart_items_service_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
