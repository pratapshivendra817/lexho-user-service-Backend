ALTER TABLE cook_coverage
    ADD COLUMN service_id BIGINT NULL AFTER id;

CREATE INDEX idx_cook_coverage_service_active_sort
    ON cook_coverage (service_id, is_active, sort_order);

CREATE INDEX idx_cook_coverage_service_family
    ON cook_coverage (service_id, family_size);
