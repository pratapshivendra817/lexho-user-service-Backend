DROP VIEW IF EXISTS serviceability_rules;

CREATE TABLE IF NOT EXISTS serviceability_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    city_id BIGINT NULL,
    zone_id BIGINT NULL,
    polygon_json LONGTEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_serviceability_rules_pincode ON serviceability_rules (pincode);
CREATE INDEX idx_serviceability_rules_city ON serviceability_rules (city);
CREATE INDEX idx_serviceability_rules_active_deleted ON serviceability_rules (active, deleted);

INSERT INTO serviceability_rules
(name, city, state, pincode, city_id, zone_id, polygon_json, active, deleted, created_at, updated_at)
SELECT sa.name, sa.city, sa.state, sa.pincode, sa.city_id, sa.zone_id, sa.polygon_json, sa.active, sa.deleted, sa.created_at, sa.updated_at
FROM serviceable_areas sa
WHERE NOT EXISTS (
    SELECT 1
    FROM serviceability_rules sr
    WHERE sr.name = sa.name
      AND sr.pincode = sa.pincode
      AND COALESCE(sr.city_id, 0) = COALESCE(sa.city_id, 0)
      AND COALESCE(sr.zone_id, 0) = COALESCE(sa.zone_id, 0)
);
