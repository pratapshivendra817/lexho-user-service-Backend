CREATE TABLE IF NOT EXISTS website_cities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    display_order INT NOT NULL DEFAULT 999,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_website_cities_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_website_cities_active_order
    ON website_cities (is_active, is_deleted, display_order, name);

INSERT INTO website_cities (
    id,
    name,
    slug,
    display_order,
    is_active,
    is_deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT DISTINCT
    c.id,
    c.name,
    LOWER(REPLACE(TRIM(c.name), ' ', '-')),
    999,
    b'1',
    b'0',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL,
    NULL
FROM website_areas wa
JOIN cities c ON c.id = wa.city_id
LEFT JOIN website_cities wc ON wc.id = c.id
WHERE wc.id IS NULL;

ALTER TABLE website_areas DROP FOREIGN KEY fk_website_areas_city;

ALTER TABLE website_areas
    ADD CONSTRAINT fk_website_areas_city
    FOREIGN KEY (city_id) REFERENCES website_cities (id);
