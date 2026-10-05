CREATE TABLE IF NOT EXISTS legal_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(120) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    title VARCHAR(160) NOT NULL,
    format VARCHAR(30) NOT NULL DEFAULT 'MARKDOWN',
    summary VARCHAR(500) NULL,
    content LONGTEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(80) NULL,
    updated_by VARCHAR(80) NULL,
    change_note VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_legal_documents_slug_platform UNIQUE (slug, platform)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_legal_documents_platform_status
    ON legal_documents(platform, status, is_active);

INSERT INTO legal_documents (
    slug, platform, document_type, title, format, summary, content, version, status,
    published_at, created_at, updated_at, created_by, updated_by, change_note, is_active
) VALUES
('terms-and-conditions', 'PARTNER', 'TERMS_AND_CONDITIONS', 'Terms & Conditions', 'MARKDOWN',
 'Applicable to all partners using the Lexho partner app.',
 '# Terms & Conditions\n\nPartner terms will be published here.',
 1, 'DRAFT', NULL, NOW(6), NOW(6), 'SYSTEM', 'SYSTEM', 'Initial placeholder', TRUE),
('privacy-policy', 'PARTNER', 'PRIVACY_POLICY', 'Privacy Policy', 'MARKDOWN',
 'Privacy policy for partners using the Lexho partner app.',
 '# Privacy Policy\n\nPartner privacy policy will be published here.',
 1, 'DRAFT', NULL, NOW(6), NOW(6), 'SYSTEM', 'SYSTEM', 'Initial placeholder', TRUE),
('about-us', 'PARTNER', 'ABOUT_US', 'About Us', 'MARKDOWN',
 'About Lexho for partners.',
 '# About Us\n\nAbout Lexho content will be published here.',
 1, 'DRAFT', NULL, NOW(6), NOW(6), 'SYSTEM', 'SYSTEM', 'Initial placeholder', TRUE)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    document_type = VALUES(document_type),
    format = VALUES(format),
    summary = VALUES(summary),
    updated_at = NOW(6),
    updated_by = 'SYSTEM';
