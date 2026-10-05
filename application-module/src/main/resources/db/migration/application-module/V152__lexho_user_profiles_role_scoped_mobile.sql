SET @has_lexho_user_profiles := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_user_profiles'
);

SET @has_mobile_unique := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_user_profiles'
      AND index_name = 'uq_lexho_user_profiles_mobile'
);

SET @sql_drop_mobile_unique := IF(
    @has_lexho_user_profiles = 1 AND @has_mobile_unique > 0,
    'ALTER TABLE lexho_user_profiles DROP INDEX uq_lexho_user_profiles_mobile',
    'SELECT 1'
);
PREPARE stmt_drop_mobile_unique FROM @sql_drop_mobile_unique;
EXECUTE stmt_drop_mobile_unique;
DEALLOCATE PREPARE stmt_drop_mobile_unique;

SET @has_mobile_role_unique := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_user_profiles'
      AND index_name = 'uq_lexho_user_profiles_mobile_role'
);

SET @sql_add_mobile_role_unique := IF(
    @has_lexho_user_profiles = 1 AND @has_mobile_role_unique = 0,
    'ALTER TABLE lexho_user_profiles ADD CONSTRAINT uq_lexho_user_profiles_mobile_role UNIQUE (mobile, role)',
    'SELECT 1'
);
PREPARE stmt_add_mobile_role_unique FROM @sql_add_mobile_role_unique;
EXECUTE stmt_add_mobile_role_unique;
DEALLOCATE PREPARE stmt_add_mobile_role_unique;
