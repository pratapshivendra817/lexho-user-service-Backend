CREATE TABLE IF NOT EXISTS sub_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NULL,
    icon VARCHAR(255) NULL,
    category_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_sub_category_category_id (category_id),
    CONSTRAINT fk_sub_category_category
        FOREIGN KEY (category_id) REFERENCES service_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
