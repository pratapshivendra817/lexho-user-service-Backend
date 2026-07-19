CREATE TABLE IF NOT EXISTS admin_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL UNIQUE,
    icon_url VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE services
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN description VARCHAR(1000) NULL,
    ADD COLUMN base_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN duration_minutes INT NOT NULL DEFAULT 30,
    ADD COLUMN avg_rating DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN total_bookings BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN image_url VARCHAR(500) NULL,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO admin_categories (name, is_active, is_deleted)
SELECT 'Cook', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM admin_categories WHERE LOWER(name) = 'cook');

INSERT INTO admin_categories (name, is_active, is_deleted)
SELECT 'Cleaning', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM admin_categories WHERE LOWER(name) = 'cleaning');

INSERT INTO admin_categories (name, is_active, is_deleted)
SELECT 'Repair', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM admin_categories WHERE LOWER(name) = 'repair');

INSERT INTO admin_categories (name, is_active, is_deleted)
SELECT 'All Services', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM admin_categories WHERE LOWER(name) = 'all services');

UPDATE services s
JOIN admin_categories c ON LOWER(TRIM(s.category_name)) = LOWER(TRIM(c.name))
SET s.category_id = c.id
WHERE s.category_id IS NULL
  AND s.category_name IS NOT NULL;

UPDATE services
SET is_active = CASE WHEN UPPER(COALESCE(status, 'ACTIVE')) = 'ACTIVE' THEN TRUE ELSE FALSE END
WHERE is_active IS NULL OR is_active IN (FALSE, TRUE);

CREATE INDEX idx_admin_categories_active_deleted ON admin_categories (is_active, is_deleted);
CREATE INDEX idx_services_category_deleted ON services (category_id, is_deleted);
CREATE INDEX idx_services_active_deleted ON services (is_active, is_deleted);
CREATE INDEX idx_services_total_bookings ON services (total_bookings);
CREATE INDEX idx_services_avg_rating ON services (avg_rating);
