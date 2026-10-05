SET @legacy_template_unique_index := (
    SELECT index_name
    FROM (
        SELECT s.index_name,
               COUNT(*) AS column_count,
               SUM(CASE WHEN s.column_name = 'template_name' THEN 1 ELSE 0 END) AS template_name_columns,
               MAX(s.non_unique) AS non_unique
        FROM information_schema.statistics s
        WHERE s.table_schema = DATABASE()
          AND s.table_name = 'notification_templates'
        GROUP BY s.index_name
    ) indexes
    WHERE non_unique = 0
      AND column_count = 1
      AND template_name_columns = 1
    LIMIT 1
);

SET @sql_drop_legacy_template_unique := IF(
    @legacy_template_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE notification_templates DROP INDEX `', REPLACE(@legacy_template_unique_index, '`', '``'), '`')
);
PREPARE stmt_drop_legacy_template_unique FROM @sql_drop_legacy_template_unique;
EXECUTE stmt_drop_legacy_template_unique;
DEALLOCATE PREPARE stmt_drop_legacy_template_unique;

SET @sql_add_template_language_unique := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'notification_templates'
          AND index_name = 'uq_notification_templates_name_language'
    ),
    'CREATE UNIQUE INDEX uq_notification_templates_name_language ON notification_templates(template_name, language)',
    'SELECT 1'
);
PREPARE stmt_add_template_language_unique FROM @sql_add_template_language_unique;
EXECUTE stmt_add_template_language_unique;
DEALLOCATE PREPARE stmt_add_template_language_unique;
