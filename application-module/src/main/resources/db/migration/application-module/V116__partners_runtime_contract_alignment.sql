SET @add_is_online = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND column_name = 'is_online'
        ),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN is_online BOOLEAN NOT NULL DEFAULT FALSE'
    )
);
PREPARE stmt FROM @add_is_online;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_is_verified = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND column_name = 'is_verified'
        ),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT FALSE'
    )
);
PREPARE stmt FROM @add_is_verified;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_profile_image = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND column_name = 'profile_image'
        ),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN profile_image VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @add_profile_image;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_service_category = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND column_name = 'service_category'
        ),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN service_category VARCHAR(80) NULL'
    )
);
PREPARE stmt FROM @add_service_category;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_rating = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'partners'
              AND column_name = 'rating'
        ),
        'SELECT 1',
        'ALTER TABLE partners ADD COLUMN rating DOUBLE NOT NULL DEFAULT 0.0'
    )
);
PREPARE stmt FROM @add_rating;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE partners
SET is_verified = CASE
    WHEN UPPER(COALESCE(onboarding_status, 'PENDING')) = 'APPROVED' THEN 1
    ELSE 0
END;

UPDATE partners
SET rating = COALESCE(rating, 0.0);

UPDATE partners p
LEFT JOIN service_category sc ON sc.id = p.service_category_id
SET p.service_category = COALESCE(NULLIF(p.service_category, ''), sc.name)
WHERE p.service_category IS NULL OR p.service_category = '';
