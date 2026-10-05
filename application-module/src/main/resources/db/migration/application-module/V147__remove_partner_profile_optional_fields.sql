SET @drop_idx_partners_email := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'partners' AND index_name = 'idx_partners_email') > 0,
    'ALTER TABLE partners DROP INDEX idx_partners_email',
    'SELECT 1'
);
PREPARE stmt FROM @drop_idx_partners_email;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_idx_partners_city := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'partners' AND index_name = 'idx_partners_city') > 0,
    'ALTER TABLE partners DROP INDEX idx_partners_city',
    'SELECT 1'
);
PREPARE stmt FROM @drop_idx_partners_city;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_partner_profile_columns := (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT 1',
        CONCAT(
            'ALTER TABLE partners ',
            GROUP_CONCAT(CONCAT('DROP COLUMN ', column_name) ORDER BY ordinal_position SEPARATOR ', ')
        )
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partners'
      AND column_name IN (
          'email',
          'gender',
          'date_of_birth',
          'current_city',
          'current_state',
          'current_full_address',
          'city',
          'state',
          'pincode',
          'full_address',
          'profile_image'
      )
);
PREPARE stmt FROM @drop_partner_profile_columns;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
