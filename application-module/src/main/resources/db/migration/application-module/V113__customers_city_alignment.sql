SET @schema_name = DATABASE();

SET @city_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'customers'
      AND column_name = 'city'
);

SET @alter_sql = IF(
    @city_exists = 0,
    'ALTER TABLE customers ADD COLUMN city VARCHAR(100) NULL AFTER profile_image',
    'SELECT 1'
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
