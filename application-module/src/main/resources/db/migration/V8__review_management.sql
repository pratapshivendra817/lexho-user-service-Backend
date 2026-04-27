SET @has_reviews_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'reviews'
);

SET @sql_create_reviews := IF(
    @has_reviews_table = 0,
    'CREATE TABLE reviews (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        booking_id BIGINT NULL,
        customer_id BIGINT NULL,
        provider_id BIGINT NULL,
        service_id BIGINT NULL,
        rating INT NOT NULL,
        review_comment VARCHAR(1000) NULL,
        review_images LONGTEXT NULL,
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
        report_count BIGINT NOT NULL DEFAULT 0,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        deleted BOOLEAN NOT NULL DEFAULT FALSE
    )',
    'SELECT 1'
);
PREPARE stmt_create_reviews FROM @sql_create_reviews;
EXECUTE stmt_create_reviews;
DEALLOCATE PREPARE stmt_create_reviews;

SET @sql_add_booking_id := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'booking_id'
    ),
    'ALTER TABLE reviews ADD COLUMN booking_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_booking_id FROM @sql_add_booking_id;
EXECUTE stmt_add_booking_id;
DEALLOCATE PREPARE stmt_add_booking_id;

SET @sql_add_customer_id := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'customer_id'
    ),
    'ALTER TABLE reviews ADD COLUMN customer_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_customer_id FROM @sql_add_customer_id;
EXECUTE stmt_add_customer_id;
DEALLOCATE PREPARE stmt_add_customer_id;

SET @sql_add_service_id := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'service_id'
    ),
    'ALTER TABLE reviews ADD COLUMN service_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_service_id FROM @sql_add_service_id;
EXECUTE stmt_add_service_id;
DEALLOCATE PREPARE stmt_add_service_id;

SET @sql_add_review_comment := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'review_comment'
    ),
    'ALTER TABLE reviews ADD COLUMN review_comment VARCHAR(1000) NULL',
    'SELECT 1'
);
PREPARE stmt_add_review_comment FROM @sql_add_review_comment;
EXECUTE stmt_add_review_comment;
DEALLOCATE PREPARE stmt_add_review_comment;

SET @sql_add_review_images := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'review_images'
    ),
    'ALTER TABLE reviews ADD COLUMN review_images LONGTEXT NULL',
    'SELECT 1'
);
PREPARE stmt_add_review_images FROM @sql_add_review_images;
EXECUTE stmt_add_review_images;
DEALLOCATE PREPARE stmt_add_review_images;

SET @sql_add_status := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'status'
    ),
    'ALTER TABLE reviews ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING''',
    'SELECT 1'
);
PREPARE stmt_add_status FROM @sql_add_status;
EXECUTE stmt_add_status;
DEALLOCATE PREPARE stmt_add_status;

SET @sql_add_report_count := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'report_count'
    ),
    'ALTER TABLE reviews ADD COLUMN report_count BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_add_report_count FROM @sql_add_report_count;
EXECUTE stmt_add_report_count;
DEALLOCATE PREPARE stmt_add_report_count;

SET @sql_add_updated_at := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'updated_at'
    ),
    'ALTER TABLE reviews ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_updated_at FROM @sql_add_updated_at;
EXECUTE stmt_add_updated_at;
DEALLOCATE PREPARE stmt_add_updated_at;

SET @sql_add_deleted := IF(
    @has_reviews_table = 1 AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'deleted'
    ),
    'ALTER TABLE reviews ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_deleted FROM @sql_add_deleted;
EXECUTE stmt_add_deleted;
DEALLOCATE PREPARE stmt_add_deleted;

SET @sql_copy_customer := IF(
    @has_reviews_table = 1
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'user_id')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'customer_id'),
    'UPDATE reviews SET customer_id = user_id WHERE customer_id IS NULL AND user_id IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_copy_customer FROM @sql_copy_customer;
EXECUTE stmt_copy_customer;
DEALLOCATE PREPARE stmt_copy_customer;

SET @sql_copy_comment := IF(
    @has_reviews_table = 1
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'comment')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'review_comment'),
    'UPDATE reviews SET review_comment = comment WHERE review_comment IS NULL AND comment IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_copy_comment FROM @sql_copy_comment;
EXECUTE stmt_copy_comment;
DEALLOCATE PREPARE stmt_copy_comment;

SET @sql_create_review_reports := '
CREATE TABLE IF NOT EXISTS review_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    reported_by_user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
)';
PREPARE stmt_create_review_reports FROM @sql_create_review_reports;
EXECUTE stmt_create_review_reports;
DEALLOCATE PREPARE stmt_create_review_reports;

SET @sql_idx_reviews_status := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_status'
    ),
    'CREATE INDEX idx_reviews_status ON reviews(status)',
    'SELECT 1'
);
PREPARE stmt_idx_reviews_status FROM @sql_idx_reviews_status;
EXECUTE stmt_idx_reviews_status;
DEALLOCATE PREPARE stmt_idx_reviews_status;

SET @sql_idx_reviews_provider := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_provider_id'
    ),
    'CREATE INDEX idx_reviews_provider_id ON reviews(provider_id)',
    'SELECT 1'
);
PREPARE stmt_idx_reviews_provider FROM @sql_idx_reviews_provider;
EXECUTE stmt_idx_reviews_provider;
DEALLOCATE PREPARE stmt_idx_reviews_provider;

SET @sql_idx_reviews_service := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_service_id'
    ),
    'CREATE INDEX idx_reviews_service_id ON reviews(service_id)',
    'SELECT 1'
);
PREPARE stmt_idx_reviews_service FROM @sql_idx_reviews_service;
EXECUTE stmt_idx_reviews_service;
DEALLOCATE PREPARE stmt_idx_reviews_service;

SET @sql_idx_reviews_booking := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_booking_id'
    ),
    'CREATE INDEX idx_reviews_booking_id ON reviews(booking_id)',
    'SELECT 1'
);
PREPARE stmt_idx_reviews_booking FROM @sql_idx_reviews_booking;
EXECUTE stmt_idx_reviews_booking;
DEALLOCATE PREPARE stmt_idx_reviews_booking;

SET @sql_idx_review_reports_review := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND index_name = 'idx_review_reports_review_id'
    ),
    'CREATE INDEX idx_review_reports_review_id ON review_reports(review_id)',
    'SELECT 1'
);
PREPARE stmt_idx_review_reports_review FROM @sql_idx_review_reports_review;
EXECUTE stmt_idx_review_reports_review;
DEALLOCATE PREPARE stmt_idx_review_reports_review;
