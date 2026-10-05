CREATE TABLE IF NOT EXISTS cook_family_sizes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_size VARCHAR(30) NOT NULL UNIQUE,
    family_size_label VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cook_family_sizes_active_order (active, display_order)
);

INSERT INTO cook_family_sizes (id, family_size, family_size_label, active, display_order)
VALUES
    (1, 'SMALL', '1-2 Members', TRUE, 1),
    (2, 'MEDIUM', '3-5 Members', TRUE, 2),
    (3, 'LARGE', '6-8 Members', TRUE, 3),
    (4, 'XLARGE', '9+ Members', TRUE, 4)
AS incoming
ON DUPLICATE KEY UPDATE
    family_size_label = incoming.family_size_label,
    active = incoming.active,
    display_order = incoming.display_order;
