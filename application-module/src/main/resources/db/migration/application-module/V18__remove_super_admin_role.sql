UPDATE admins SET role = 'ADMIN' WHERE role = 'SUPER_ADMIN';
UPDATE customers SET role = 'ADMIN' WHERE role = 'SUPER_ADMIN';

SET @has_lexho_user_profiles := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_user_profiles'
);

SET @sql_update_lexho_user_profiles := IF(
    @has_lexho_user_profiles = 1,
    'UPDATE lexho_user_profiles SET role = ''ADMIN'' WHERE role = ''SUPER_ADMIN''',
    'SELECT 1'
);
PREPARE stmt_update_lexho_user_profiles FROM @sql_update_lexho_user_profiles;
EXECUTE stmt_update_lexho_user_profiles;
DEALLOCATE PREPARE stmt_update_lexho_user_profiles;

ALTER TABLE admins
    MODIFY COLUMN role ENUM ('ADMIN','CUSTOMER','PARTNER') NOT NULL;

ALTER TABLE customers
    MODIFY COLUMN role ENUM ('ADMIN','CUSTOMER','PARTNER') NOT NULL;

SET @sql_alter_lexho_user_profiles := IF(
    @has_lexho_user_profiles = 1,
    'ALTER TABLE lexho_user_profiles MODIFY COLUMN role ENUM (''ADMIN'',''CUSTOMER'',''PARTNER'') NOT NULL',
    'SELECT 1'
);
PREPARE stmt_alter_lexho_user_profiles FROM @sql_alter_lexho_user_profiles;
EXECUTE stmt_alter_lexho_user_profiles;
DEALLOCATE PREPARE stmt_alter_lexho_user_profiles;
