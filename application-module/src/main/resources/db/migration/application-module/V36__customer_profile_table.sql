SET @sql_create_customers := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
    ),
    'SELECT 1',
    'CREATE TABLE customers (
        id BIGINT PRIMARY KEY,
        full_name VARCHAR(120) NOT NULL,
        email VARCHAR(150) NULL,
        phone_number VARCHAR(20) NOT NULL,
        gender VARCHAR(10) NULL,
        profile_image_url VARCHAR(512) NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT chk_customers_gender CHECK (gender IN (''MALE'', ''FEMALE'', ''OTHER'') OR gender IS NULL),
        CONSTRAINT uk_customers_phone_number UNIQUE (phone_number),
        CONSTRAINT uk_customers_email UNIQUE (email)
    )'
);
PREPARE stmt_create_customers FROM @sql_create_customers;
EXECUTE stmt_create_customers;
DEALLOCATE PREPARE stmt_create_customers;
