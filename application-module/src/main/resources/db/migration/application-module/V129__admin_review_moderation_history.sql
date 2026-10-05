SET @sql_add_review_report_status := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND column_name = 'status'),
    'ALTER TABLE review_reports ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''OPEN''',
    'SELECT 1'
);
PREPARE stmt_add_review_report_status FROM @sql_add_review_report_status;
EXECUTE stmt_add_review_report_status;
DEALLOCATE PREPARE stmt_add_review_report_status;

SET @sql_add_review_report_resolved_by := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND column_name = 'resolved_by_admin_id'),
    'ALTER TABLE review_reports ADD COLUMN resolved_by_admin_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_review_report_resolved_by FROM @sql_add_review_report_resolved_by;
EXECUTE stmt_add_review_report_resolved_by;
DEALLOCATE PREPARE stmt_add_review_report_resolved_by;

SET @sql_add_review_report_resolution_note := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND column_name = 'resolution_note'),
    'ALTER TABLE review_reports ADD COLUMN resolution_note VARCHAR(1000) NULL',
    'SELECT 1'
);
PREPARE stmt_add_review_report_resolution_note FROM @sql_add_review_report_resolution_note;
EXECUTE stmt_add_review_report_resolution_note;
DEALLOCATE PREPARE stmt_add_review_report_resolution_note;

SET @sql_add_review_report_resolved_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND column_name = 'resolved_at'),
    'ALTER TABLE review_reports ADD COLUMN resolved_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_review_report_resolved_at FROM @sql_add_review_report_resolved_at;
EXECUTE stmt_add_review_report_resolved_at;
DEALLOCATE PREPARE stmt_add_review_report_resolved_at;

SET @sql_add_review_report_updated_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND column_name = 'updated_at'),
    'ALTER TABLE review_reports ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_review_report_updated_at FROM @sql_add_review_report_updated_at;
EXECUTE stmt_add_review_report_updated_at;
DEALLOCATE PREPARE stmt_add_review_report_updated_at;

CREATE TABLE IF NOT EXISTS review_moderation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    action VARCHAR(40) NOT NULL,
    admin_id BIGINT NULL,
    admin_name VARCHAR(180) NULL,
    admin_email VARCHAR(180) NULL,
    reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SET @sql_idx_review_moderation_history_review := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_moderation_history' AND index_name = 'idx_review_moderation_history_review'),
    'CREATE INDEX idx_review_moderation_history_review ON review_moderation_history(review_id)',
    'SELECT 1'
);
PREPARE stmt_idx_review_moderation_history_review FROM @sql_idx_review_moderation_history_review;
EXECUTE stmt_idx_review_moderation_history_review;
DEALLOCATE PREPARE stmt_idx_review_moderation_history_review;

SET @sql_idx_review_reports_status := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_reports' AND index_name = 'idx_review_reports_status'),
    'CREATE INDEX idx_review_reports_status ON review_reports(status)',
    'SELECT 1'
);
PREPARE stmt_idx_review_reports_status FROM @sql_idx_review_reports_status;
EXECUTE stmt_idx_review_reports_status;
DEALLOCATE PREPARE stmt_idx_review_reports_status;
