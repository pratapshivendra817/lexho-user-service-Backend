CREATE TABLE IF NOT EXISTS cleaning_coverage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    duration_hours DECIMAL(5,2) NOT NULL,
    reference VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cleaning_coverage_duration (duration_hours),
    INDEX idx_cleaning_coverage_active_sort (is_active, sort_order)
);

CREATE TABLE IF NOT EXISTS cleaning_coverage_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coverage_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    estimated_minutes INT NOT NULL,
    icon VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_cleaning_coverage_task_coverage_sort (coverage_id, sort_order),
    CONSTRAINT fk_cleaning_coverage_task_coverage
        FOREIGN KEY (coverage_id) REFERENCES cleaning_coverage(id)
        ON DELETE CASCADE
);

INSERT INTO cleaning_coverage (duration_hours, reference, sort_order, is_active)
SELECT 0.50, '2BHK', 50, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cleaning_coverage WHERE duration_hours = 0.50);

INSERT INTO cleaning_coverage (duration_hours, reference, sort_order, is_active)
SELECT 1.00, '2BHK', 100, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cleaning_coverage WHERE duration_hours = 1.00);

INSERT INTO cleaning_coverage (duration_hours, reference, sort_order, is_active)
SELECT 1.50, '2BHK', 150, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cleaning_coverage WHERE duration_hours = 1.50);

INSERT INTO cleaning_coverage (duration_hours, reference, sort_order, is_active)
SELECT 2.00, '2BHK', 200, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cleaning_coverage WHERE duration_hours = 2.00);

INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Utensils', 20, 'utensils', 1 FROM cleaning_coverage WHERE duration_hours = 0.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Dusting & Wiping', 15, 'dusting', 2 FROM cleaning_coverage WHERE duration_hours = 0.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Balcony Cleaning', 15, 'balcony', 3 FROM cleaning_coverage WHERE duration_hours = 0.50;

INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Bathroom Cleaning', 40, 'bathroom', 1 FROM cleaning_coverage WHERE duration_hours = 1.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Utensils', 20, 'utensils', 2 FROM cleaning_coverage WHERE duration_hours = 1.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Balcony Cleaning', 30, 'balcony', 3 FROM cleaning_coverage WHERE duration_hours = 1.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Fan Cleaning', 20, 'fan', 4 FROM cleaning_coverage WHERE duration_hours = 1.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Dusting & Wiping', 30, 'dusting', 5 FROM cleaning_coverage WHERE duration_hours = 1.00;

INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Bathroom Cleaning', 40, 'bathroom', 1 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Utensils', 20, 'utensils', 2 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Balcony Cleaning', 30, 'balcony', 3 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Fan Cleaning', 20, 'fan', 4 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Dusting & Wiping', 30, 'dusting', 5 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Sweeping & Mopping', 30, 'mopping', 6 FROM cleaning_coverage WHERE duration_hours = 1.50;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Window Cleaning', 25, 'window', 7 FROM cleaning_coverage WHERE duration_hours = 1.50;

INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Bathroom Cleaning', 40, 'bathroom', 1 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Utensils', 20, 'utensils', 2 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Balcony Cleaning', 30, 'balcony', 3 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Fan Cleaning', 20, 'fan', 4 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Dusting & Wiping', 30, 'dusting', 5 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Sweeping & Mopping', 30, 'mopping', 6 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Window Cleaning', 25, 'window', 7 FROM cleaning_coverage WHERE duration_hours = 2.00;
INSERT INTO cleaning_coverage_task (coverage_id, name, estimated_minutes, icon, sort_order)
SELECT id, 'Kitchen Cleaning', 30, 'kitchen', 8 FROM cleaning_coverage WHERE duration_hours = 2.00;
