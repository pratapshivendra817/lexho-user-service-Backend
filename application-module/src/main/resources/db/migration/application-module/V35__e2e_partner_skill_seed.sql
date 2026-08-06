SET @has_partner_service_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_service'
);

SET @sql := IF(
    @has_partner_service_table = 1,
    'INSERT INTO partner_service (partner_id, sub_service_id, created_at)
     SELECT 4, 1, NOW()
     WHERE EXISTS (SELECT 1 FROM partners WHERE id = 4)
       AND EXISTS (SELECT 1 FROM services WHERE id = 1)
       AND NOT EXISTS (
         SELECT 1
         FROM partner_service
         WHERE partner_id = 4
           AND sub_service_id = 1
     )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
