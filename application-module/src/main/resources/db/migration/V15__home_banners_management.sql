CREATE TABLE IF NOT EXISTS home_banners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(500) NULL,
    image_url VARCHAR(1000) NOT NULL,
    redirect_type VARCHAR(20) NOT NULL,
    redirect_id BIGINT NULL,
    priority INT NOT NULL DEFAULT 100,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    banner_type VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_home_banners_active_priority (is_active, priority)
);
