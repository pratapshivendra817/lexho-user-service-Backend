CREATE TABLE website_category_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    show_on_website BIT(1) NOT NULL DEFAULT b'0',
    display_order INT NOT NULL DEFAULT 999,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_website_category_config_category UNIQUE (category_id),
    CONSTRAINT fk_website_category_config_category FOREIGN KEY (category_id) REFERENCES admin_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_website_category_config_visible_order
    ON website_category_config (show_on_website, display_order, category_id);

CREATE TABLE website_city_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    city_id BIGINT NOT NULL,
    show_on_website BIT(1) NOT NULL DEFAULT b'0',
    display_order INT NOT NULL DEFAULT 999,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_website_city_config_city UNIQUE (city_id),
    CONSTRAINT fk_website_city_config_city FOREIGN KEY (city_id) REFERENCES cities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_website_city_config_visible_order
    ON website_city_config (show_on_website, display_order, city_id);

CREATE TABLE website_areas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    city_id BIGINT NOT NULL,
    area_name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    display_order INT NOT NULL DEFAULT 999,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_website_areas_city_slug UNIQUE (city_id, slug),
    CONSTRAINT fk_website_areas_city FOREIGN KEY (city_id) REFERENCES cities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_website_areas_city_active
    ON website_areas (city_id, is_active, is_deleted, display_order, area_name);
