SET @partners_total_reviews_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partners'
      AND column_name = 'total_reviews'
);
SET @sql := IF(@partners_total_reviews_exists = 0,
    'ALTER TABLE partners ADD COLUMN total_reviews BIGINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @partner_total_reviews_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner'
      AND column_name = 'total_reviews'
);
SET @sql := IF(@partner_total_reviews_exists = 0,
    'ALTER TABLE partner ADD COLUMN total_reviews BIGINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @review_comment_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reviews'
      AND column_name = 'review_comment'
);
SET @sql := IF(@review_comment_exists = 1,
    'ALTER TABLE reviews MODIFY COLUMN review_comment VARCHAR(500) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @unique_booking_review_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reviews'
      AND index_name = 'uk_reviews_booking_id'
);
SET @sql := IF(@unique_booking_review_exists = 0,
    'CREATE UNIQUE INDEX uk_reviews_booking_id ON reviews(booking_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
