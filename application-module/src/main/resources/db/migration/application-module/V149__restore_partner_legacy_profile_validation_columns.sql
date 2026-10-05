SET @add_partner_email = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'email'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN email VARCHAR(150) NULL'
    )
);
PREPARE stmt FROM @add_partner_email;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_current_city = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'current_city'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN current_city VARCHAR(100) NULL'
    )
);
PREPARE stmt FROM @add_current_city;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_current_state = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'current_state'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN current_state VARCHAR(100) NULL'
    )
);
PREPARE stmt FROM @add_current_state;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_current_full_address = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'current_full_address'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN current_full_address VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @add_current_full_address;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_full_address = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'full_address'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN full_address VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @add_full_address;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_profile_image = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'profile_image'),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN profile_image VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_profile_image;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
