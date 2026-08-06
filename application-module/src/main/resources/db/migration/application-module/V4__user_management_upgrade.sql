CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL DEFAULT 'Guest User',
    email VARCHAR(150) NULL,
    phone_number VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL DEFAULT 'OTP_LOGIN_USER',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    profile_image VARCHAR(500) NULL,
    state VARCHAR(100) NULL,
    address VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_bookings BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_customers_phone_number UNIQUE (phone_number),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

SET @has_customers_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
);

SET @sql_add_phone_number := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'phone_number'
    ),
    'ALTER TABLE customers ADD COLUMN phone_number VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt_add_phone_number FROM @sql_add_phone_number;
EXECUTE stmt_add_phone_number;
DEALLOCATE PREPARE stmt_add_phone_number;

SET @sql_add_email := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'email'
    ),
    'ALTER TABLE customers ADD COLUMN email VARCHAR(150) NULL',
    'SELECT 1'
);
PREPARE stmt_add_email FROM @sql_add_email;
EXECUTE stmt_add_email;
DEALLOCATE PREPARE stmt_add_email;

SET @sql_add_password := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'password'
    ),
    'ALTER TABLE customers ADD COLUMN password VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_add_password FROM @sql_add_password;
EXECUTE stmt_add_password;
DEALLOCATE PREPARE stmt_add_password;

SET @sql_add_profile_image := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'profile_image'
    ),
    'ALTER TABLE customers ADD COLUMN profile_image VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt_add_profile_image FROM @sql_add_profile_image;
EXECUTE stmt_add_profile_image;
DEALLOCATE PREPARE stmt_add_profile_image;

SET @sql_add_state := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'state'
    ),
    'ALTER TABLE customers ADD COLUMN state VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_state FROM @sql_add_state;
EXECUTE stmt_add_state;
DEALLOCATE PREPARE stmt_add_state;

SET @sql_add_address := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'address'
    ),
    'ALTER TABLE customers ADD COLUMN address VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt_add_address FROM @sql_add_address;
EXECUTE stmt_add_address;
DEALLOCATE PREPARE stmt_add_address;

SET @sql_add_status := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'status'
    ),
    'ALTER TABLE customers ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE''',
    'SELECT 1'
);
PREPARE stmt_add_status FROM @sql_add_status;
EXECUTE stmt_add_status;
DEALLOCATE PREPARE stmt_add_status;

SET @sql_add_total_bookings := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'total_bookings'
    ),
    'ALTER TABLE customers ADD COLUMN total_bookings BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_add_total_bookings FROM @sql_add_total_bookings;
EXECUTE stmt_add_total_bookings;
DEALLOCATE PREPARE stmt_add_total_bookings;

SET @sql_add_deleted := IF(
    @has_customers_table = 1 AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND column_name = 'deleted'
    ),
    'ALTER TABLE customers ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_deleted FROM @sql_add_deleted;
EXECUTE stmt_add_deleted;
DEALLOCATE PREPARE stmt_add_deleted;

SET @has_phone_number := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'phone_number'
);
SET @has_mobile := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'mobile'
);
SET @sql_copy_mobile := IF(
    @has_customers_table = 1 AND @has_phone_number = 1 AND @has_mobile = 1,
    'UPDATE customers SET phone_number = mobile WHERE (phone_number IS NULL OR phone_number = '''') AND mobile IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_copy_mobile FROM @sql_copy_mobile;
EXECUTE stmt_copy_mobile;
DEALLOCATE PREPARE stmt_copy_mobile;

SET @has_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'status'
);
SET @has_active := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'active'
);
SET @sql_normalize_status := IF(
    @has_customers_table = 1 AND @has_status = 1 AND @has_active = 1,
    'UPDATE customers SET status = CASE WHEN active = TRUE THEN ''ACTIVE'' ELSE ''INACTIVE'' END WHERE status IS NULL OR status = ''''',
    'SELECT 1'
);
PREPARE stmt_normalize_status FROM @sql_normalize_status;
EXECUTE stmt_normalize_status;
DEALLOCATE PREPARE stmt_normalize_status;

SET @has_full_name := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'full_name'
);
SET @sql_default_full_name := IF(
    @has_customers_table = 1 AND @has_full_name = 1,
    'UPDATE customers SET full_name = ''Guest User'' WHERE full_name IS NULL OR TRIM(full_name) = ''''',
    'SELECT 1'
);
PREPARE stmt_default_full_name FROM @sql_default_full_name;
EXECUTE stmt_default_full_name;
DEALLOCATE PREPARE stmt_default_full_name;

SET @has_password := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'password'
);
SET @sql_default_password := IF(
    @has_customers_table = 1 AND @has_password = 1,
    'UPDATE customers SET password = ''OTP_LOGIN_USER'' WHERE password IS NULL OR password = ''''',
    'SELECT 1'
);
PREPARE stmt_default_password FROM @sql_default_password;
EXECUTE stmt_default_password;
DEALLOCATE PREPARE stmt_default_password;

SET @sql_idx_uq_phone := IF(
    @has_customers_table = 1
    AND @has_phone_number = 1
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND index_name = 'uq_customers_phone_number'
    ),
    'CREATE UNIQUE INDEX uq_customers_phone_number ON customers(phone_number)',
    'SELECT 1'
);
PREPARE stmt_idx_uq_phone FROM @sql_idx_uq_phone;
EXECUTE stmt_idx_uq_phone;
DEALLOCATE PREPARE stmt_idx_uq_phone;

SET @has_email := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'email'
);
SET @sql_idx_uq_email := IF(
    @has_customers_table = 1
    AND @has_email = 1
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND index_name = 'uq_customers_email'
    ),
    'CREATE UNIQUE INDEX uq_customers_email ON customers(email)',
    'SELECT 1'
);
PREPARE stmt_idx_uq_email FROM @sql_idx_uq_email;
EXECUTE stmt_idx_uq_email;
DEALLOCATE PREPARE stmt_idx_uq_email;

SET @has_role := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'role'
);
SET @sql_idx_role := IF(
    @has_customers_table = 1
    AND @has_role = 1
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND index_name = 'idx_customers_role'
    ),
    'CREATE INDEX idx_customers_role ON customers(role)',
    'SELECT 1'
);
PREPARE stmt_idx_role FROM @sql_idx_role;
EXECUTE stmt_idx_role;
DEALLOCATE PREPARE stmt_idx_role;

SET @sql_idx_status := IF(
    @has_customers_table = 1
    AND @has_status = 1
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'customers'
          AND index_name = 'idx_customers_status'
    ),
    'CREATE INDEX idx_customers_status ON customers(status)',
    'SELECT 1'
);
PREPARE stmt_idx_status FROM @sql_idx_status;
EXECUTE stmt_idx_status;
DEALLOCATE PREPARE stmt_idx_status;
