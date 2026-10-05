SET @sql_create_partners := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'partners'
    ),
    'SELECT 1',
    'CREATE TABLE partners (
        id BIGINT PRIMARY KEY,
        name VARCHAR(120) NOT NULL,
        mobile_number VARCHAR(20) NOT NULL UNIQUE,
        email VARCHAR(120),
        city VARCHAR(100),
        address VARCHAR(255),
        service_category VARCHAR(80),
        rating DOUBLE NOT NULL DEFAULT 0,
        profile_image VARCHAR(1000),
        is_online BIT(1) NOT NULL DEFAULT b''0'',
        is_verified BIT(1) NOT NULL DEFAULT b''0'',
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL
    )'
);
PREPARE stmt_create_partners FROM @sql_create_partners;
EXECUTE stmt_create_partners;
DEALLOCATE PREPARE stmt_create_partners;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND index_name = 'idx_partners_email'
        ),
        'SELECT 1',
        'CREATE INDEX idx_partners_email ON partners(email)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
            GROUP BY index_name
            HAVING COUNT(*) = 1
               AND MAX(CASE WHEN seq_in_index = 1 AND column_name = 'city' THEN 1 ELSE 0 END) = 1
        ),
        'SELECT 1',
        'CREATE INDEX idx_partners_city ON partners(city)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'customer_name'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN customer_name VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'customer_phone'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN customer_phone VARCHAR(20) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'latitude'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN latitude DOUBLE NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'longitude'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN longitude DOUBLE NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'service_type'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN service_type VARCHAR(80) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'amount'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN amount DOUBLE NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'payment_mode'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN payment_mode VARCHAR(20) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'assigned_partner_id'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN assigned_partner_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'before_image_urls'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN before_image_urls LONGTEXT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND column_name = 'after_image_urls'
        ),
        'SELECT 1',
        'ALTER TABLE orders ADD COLUMN after_image_urls LONGTEXT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'orders'
              AND index_name = 'idx_orders_assigned_partner_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_orders_assigned_partner_id ON orders(assigned_partner_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'order_status_history'
              AND index_name = 'idx_order_status_history_order'
        ),
        'SELECT 1',
        'CREATE INDEX idx_order_status_history_order ON order_status_history(order_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS partner_availability (
    partner_id BIGINT PRIMARY KEY,
    is_online BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_partner_availability_partner FOREIGN KEY (partner_id) REFERENCES partners(id)
);
