CREATE TABLE IF NOT EXISTS serviceable_areas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_serviceable_areas_deleted ON serviceable_areas(deleted);
CREATE INDEX idx_serviceable_areas_active ON serviceable_areas(active);
CREATE INDEX idx_serviceable_areas_city_state ON serviceable_areas(city, state);
CREATE INDEX idx_serviceable_areas_pincode ON serviceable_areas(pincode);
