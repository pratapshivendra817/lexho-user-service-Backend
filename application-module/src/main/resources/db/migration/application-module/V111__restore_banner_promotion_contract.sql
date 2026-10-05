SET @schema_name = DATABASE();

SET @placement_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'banner_promotions'
      AND column_name = 'placement'
);

SET @redirect_type_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'banner_promotions'
      AND column_name = 'redirect_type'
);

SET @redirect_value_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'banner_promotions'
      AND column_name = 'redirect_value'
);

SET @alter_sql = 'ALTER TABLE banner_promotions';

SET @alter_sql = IF(
    @placement_exists = 0,
    CONCAT(@alter_sql, ' ADD COLUMN placement VARCHAR(20) NOT NULL DEFAULT ''TOP'' AFTER platform_type'),
    @alter_sql
);

SET @alter_sql = IF(
    @redirect_type_exists = 0,
    CONCAT(
        @alter_sql,
        IF(@alter_sql = 'ALTER TABLE banner_promotions', '', ','),
        ' ADD COLUMN redirect_type VARCHAR(30) NOT NULL DEFAULT ''NONE'' AFTER placement'
    ),
    @alter_sql
);

SET @alter_sql = IF(
    @redirect_value_exists = 0,
    CONCAT(
        @alter_sql,
        IF(@alter_sql = 'ALTER TABLE banner_promotions', '', ','),
        ' ADD COLUMN redirect_value VARCHAR(1000) NULL AFTER redirect_type'
    ),
    @alter_sql
);

SET @alter_sql = IF(
    @alter_sql = 'ALTER TABLE banner_promotions',
    'SELECT 1',
    @alter_sql
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
