CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    lock_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @has_admins_password_hash := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'admins'
      AND column_name = 'password_hash'
);

SET @sql_add_admins_password_hash := IF(
    @has_admins_password_hash = 0,
    'ALTER TABLE admins ADD COLUMN password_hash VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_add_admins_password_hash FROM @sql_add_admins_password_hash;
EXECUTE stmt_add_admins_password_hash;
DEALLOCATE PREPARE stmt_add_admins_password_hash;

SET @has_admins_password := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'admins'
      AND column_name = 'password'
);

SET @sql_copy_password := IF(
    @has_admins_password_hash = 1 AND @has_admins_password = 1,
    'UPDATE admins SET password_hash = password WHERE (password_hash IS NULL OR password_hash = '''') AND password IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_copy_password FROM @sql_copy_password;
EXECUTE stmt_copy_password;
DEALLOCATE PREPARE stmt_copy_password;

SET @sql_normalize_role := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'admins'
          AND column_name = 'role'
    ),
    'UPDATE admins SET role = UPPER(role) WHERE role IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_normalize_role FROM @sql_normalize_role;
EXECUTE stmt_normalize_role;
DEALLOCATE PREPARE stmt_normalize_role;

SET @has_partner_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'partner'
);

SET @has_partner_verification_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner'
      AND column_name = 'verification_status'
);

SET @sql_partner_verification := IF(
    @has_partner_table = 1 AND @has_partner_verification_status = 0,
    'ALTER TABLE partner ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT ''PENDING''',
    'SELECT 1'
);
PREPARE stmt_partner_verification FROM @sql_partner_verification;
EXECUTE stmt_partner_verification;
DEALLOCATE PREPARE stmt_partner_verification;

SET @has_orders_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
);

SET @has_orders_created_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'created_at'
);

SET @sql_orders_created_at := IF(
    @has_orders_table = 1 AND @has_orders_created_at = 0,
    'ALTER TABLE orders ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_orders_created_at FROM @sql_orders_created_at;
EXECUTE stmt_orders_created_at;
DEALLOCATE PREPARE stmt_orders_created_at;

SET @has_orders_updated_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND column_name = 'updated_at'
);

SET @sql_orders_updated_at := IF(
    @has_orders_table = 1 AND @has_orders_updated_at = 0,
    'ALTER TABLE orders ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_orders_updated_at FROM @sql_orders_updated_at;
EXECUTE stmt_orders_updated_at;
DEALLOCATE PREPARE stmt_orders_updated_at;
