SET @drop_profile_photo_url = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'profile_photo_url'),
        'ALTER TABLE partners DROP COLUMN profile_photo_url',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_profile_photo_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_relationship = (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'partners'
                  AND column_name = 'relationship'),
        'ALTER TABLE partners DROP COLUMN relationship',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_relationship;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
