SET @has_lexho_user_profiles := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'lexho_user_profiles'
);

SET @drop_mobile_unique_clauses := (
    SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ')
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'lexho_user_profiles'
          AND index_name <> 'PRIMARY'
        GROUP BY index_name
        HAVING COUNT(*) = 1
           AND MAX(CASE WHEN column_name = 'mobile' THEN 1 ELSE 0 END) = 1
           AND MAX(non_unique) = 0
    ) mobile_only_unique_indexes
);

SET @sql_drop_mobile_uniques := IF(
    @has_lexho_user_profiles = 1 AND @drop_mobile_unique_clauses IS NOT NULL,
    CONCAT('ALTER TABLE lexho_user_profiles ', @drop_mobile_unique_clauses),
    'SELECT 1'
);
PREPARE stmt_drop_mobile_uniques FROM @sql_drop_mobile_uniques;
EXECUTE stmt_drop_mobile_uniques;
DEALLOCATE PREPARE stmt_drop_mobile_uniques;

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
