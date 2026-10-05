CREATE TABLE social_media_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    platform VARCHAR(30) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    label VARCHAR(100) NOT NULL,
    icon VARCHAR(100) NULL,
    display_order INT NOT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_media_links_platform UNIQUE (platform),
    CONSTRAINT chk_social_media_links_display_order CHECK (display_order > 0),
    CONSTRAINT chk_social_media_links_platform CHECK (platform IN ('INSTAGRAM', 'FACEBOOK', 'YOUTUBE', 'LINKEDIN', 'X', 'OTHER')),
    CONSTRAINT chk_social_media_links_url_http CHECK (url LIKE 'http://%' OR url LIKE 'https://%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_social_media_links_active_order ON social_media_links (is_active, display_order, id);
CREATE INDEX idx_social_media_links_display_order ON social_media_links (display_order, id);
