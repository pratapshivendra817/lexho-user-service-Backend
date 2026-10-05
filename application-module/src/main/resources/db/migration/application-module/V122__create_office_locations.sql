CREATE TABLE IF NOT EXISTS office_locations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    phone VARCHAR(20) NULL,
    type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    map_provider VARCHAR(50) NULL,
    place_id VARCHAR(150) NULL,
    landmark VARCHAR(255) NULL,
    entrance_info VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(80) NULL,
    updated_by VARCHAR(80) NULL,
    active_type VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN active THEN type ELSE NULL END) STORED,
    CONSTRAINT uk_office_locations_one_active_type UNIQUE (active_type)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_office_locations_type_active
    ON office_locations(type, active, updated_at);
