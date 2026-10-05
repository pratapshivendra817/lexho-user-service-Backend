CREATE TABLE IF NOT EXISTS partner_warning_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warning_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    sender_id BIGINT NULL,
    message VARCHAR(2000) NOT NULL,
    attachment_urls LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_partner_warning_comments_warning
        FOREIGN KEY (warning_id) REFERENCES partner_penalties(id),
    INDEX idx_partner_warning_comments_warning_created_at (warning_id, created_at)
);
