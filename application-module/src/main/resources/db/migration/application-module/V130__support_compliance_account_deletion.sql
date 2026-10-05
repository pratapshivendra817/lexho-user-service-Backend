SET @sql_legal_effective_from := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND column_name = 'effective_from'),
    'ALTER TABLE legal_documents ADD COLUMN effective_from DATETIME(6) NULL AFTER published_at',
    'SELECT 1'
);
PREPARE stmt_legal_effective_from FROM @sql_legal_effective_from;
EXECUTE stmt_legal_effective_from;
DEALLOCATE PREPARE stmt_legal_effective_from;

SET @sql_legal_published_by := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND column_name = 'published_by'),
    'ALTER TABLE legal_documents ADD COLUMN published_by VARCHAR(80) NULL AFTER effective_from',
    'SELECT 1'
);
PREPARE stmt_legal_published_by FROM @sql_legal_published_by;
EXECUTE stmt_legal_published_by;
DEALLOCATE PREPARE stmt_legal_published_by;

SET @sql_legal_archived_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND column_name = 'archived_at'),
    'ALTER TABLE legal_documents ADD COLUMN archived_at DATETIME(6) NULL AFTER published_by',
    'SELECT 1'
);
PREPARE stmt_legal_archived_at FROM @sql_legal_archived_at;
EXECUTE stmt_legal_archived_at;
DEALLOCATE PREPARE stmt_legal_archived_at;

SET @sql_legal_current_published := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND column_name = 'current_published'),
    'ALTER TABLE legal_documents ADD COLUMN current_published BOOLEAN NOT NULL DEFAULT FALSE AFTER archived_at',
    'SELECT 1'
);
PREPARE stmt_legal_current_published FROM @sql_legal_current_published;
EXECUTE stmt_legal_current_published;
DEALLOCATE PREPARE stmt_legal_current_published;

SET @sql_drop_legal_slug_platform := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND index_name = 'uk_legal_documents_slug_platform'),
    'ALTER TABLE legal_documents DROP INDEX uk_legal_documents_slug_platform',
    'SELECT 1'
);
PREPARE stmt_drop_legal_slug_platform FROM @sql_drop_legal_slug_platform;
EXECUTE stmt_drop_legal_slug_platform;
DEALLOCATE PREPARE stmt_drop_legal_slug_platform;

SET @sql_legal_slug_platform_version := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND index_name = 'uk_legal_documents_slug_platform_version'),
    'ALTER TABLE legal_documents ADD CONSTRAINT uk_legal_documents_slug_platform_version UNIQUE (slug, platform, version)',
    'SELECT 1'
);
PREPARE stmt_legal_slug_platform_version FROM @sql_legal_slug_platform_version;
EXECUTE stmt_legal_slug_platform_version;
DEALLOCATE PREPARE stmt_legal_slug_platform_version;

SET @sql_legal_current_idx := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'legal_documents' AND index_name = 'idx_legal_documents_type_current'),
    'CREATE INDEX idx_legal_documents_type_current ON legal_documents(document_type, status, current_published, is_active)',
    'SELECT 1'
);
PREPARE stmt_legal_current_idx FROM @sql_legal_current_idx;
EXECUTE stmt_legal_current_idx;
DEALLOCATE PREPARE stmt_legal_current_idx;

UPDATE legal_documents
SET current_published = TRUE,
    effective_from = COALESCE(effective_from, published_at)
WHERE status = 'PUBLISHED'
  AND is_active = TRUE
  AND current_published = FALSE;

CREATE TABLE IF NOT EXISTS legal_acceptances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(30) NOT NULL,
    document_type VARCHAR(80) NOT NULL,
    document_version INT NOT NULL,
    accepted_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_legal_acceptance_user_doc_version UNIQUE (user_id, user_type, document_type, document_version)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_legal_acceptances_user
    ON legal_acceptances(user_id, user_type, accepted_at);

SET @sql_faq_user_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'support_faqs' AND column_name = 'user_type'),
    'ALTER TABLE support_faqs ADD COLUMN user_type VARCHAR(30) NOT NULL DEFAULT ''GENERAL'' AFTER display_order',
    'SELECT 1'
);
PREPARE stmt_faq_user_type FROM @sql_faq_user_type;
EXECUTE stmt_faq_user_type;
DEALLOCATE PREPARE stmt_faq_user_type;

SET @sql_faq_user_type_idx := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'support_faqs' AND index_name = 'idx_support_faqs_user_type_active'),
    'CREATE INDEX idx_support_faqs_user_type_active ON support_faqs(user_type, active, display_order)',
    'SELECT 1'
);
PREPARE stmt_faq_user_type_idx FROM @sql_faq_user_type_idx;
EXECUTE stmt_faq_user_type_idx;
DEALLOCATE PREPARE stmt_faq_user_type_idx;

CREATE TABLE IF NOT EXISTS account_deletion_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    request_reason VARCHAR(500) NULL,
    rejection_reason VARCHAR(500) NULL,
    active_bookings BIGINT NOT NULL DEFAULT 0,
    pending_payments BIGINT NOT NULL DEFAULT 0,
    pending_refunds BIGINT NOT NULL DEFAULT 0,
    pending_partner_payouts BIGINT NOT NULL DEFAULT 0,
    requested_at DATETIME(6) NOT NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    processed_by_admin_id BIGINT NULL,
    processed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_account_deletion_user_status
    ON account_deletion_requests(user_id, user_type, status);

CREATE INDEX idx_account_deletion_status_requested
    ON account_deletion_requests(status, requested_at);
