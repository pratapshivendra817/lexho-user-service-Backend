ALTER TABLE services
    ADD COLUMN service_type VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS service_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    type ENUM('INCLUDED', 'EXCLUDED') NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_details_service
        FOREIGN KEY (service_id) REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_service_details_service_id ON service_details(service_id);
CREATE INDEX idx_service_details_type ON service_details(type);
