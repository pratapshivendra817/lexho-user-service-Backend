CREATE TABLE IF NOT EXISTS sections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NULL,
    icon VARCHAR(255) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sections_active (is_active),
    INDEX idx_sections_type (type)
);

CREATE TABLE IF NOT EXISTS section_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    section_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    value VARCHAR(255) NULL,
    icon VARCHAR(255) NOT NULL,
    priority INT NOT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT fk_section_items_section
        FOREIGN KEY (section_id) REFERENCES sections(id)
        ON DELETE CASCADE,
    INDEX idx_section_items_section_priority (section_id, priority),
    INDEX idx_section_items_active (is_active)
);
