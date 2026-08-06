SET @addr_pincode := (
    SELECT pincode
    FROM lexho_addresses
    WHERE id = 3
    LIMIT 1
);

SET @has_partners_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'partners'
);

SET @has_partners_is_online := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partners'
      AND column_name = 'is_online'
);

SET @sql := IF(
    @has_partners_table = 1 AND @has_partners_is_online = 1,
    'UPDATE partners SET is_online = TRUE, is_verified = TRUE WHERE id = 4',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_partner_shifts_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_shifts'
);

SET @sql := IF(
    @has_partner_shifts_table = 1,
    'UPDATE partner_shifts SET is_online = TRUE, current_pincode = ? WHERE partner_id = 4',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
SET @p_addr_pincode := @addr_pincode;
EXECUTE stmt USING @p_addr_pincode;
DEALLOCATE PREPARE stmt;
