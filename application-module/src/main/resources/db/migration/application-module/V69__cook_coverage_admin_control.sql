CREATE TABLE IF NOT EXISTS cook_coverage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_size VARCHAR(20) NOT NULL,
    members VARCHAR(30) NOT NULL,
    family_size_label VARCHAR(50) NOT NULL,
    reference VARCHAR(150) NULL,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cook_coverage_active_sort (is_active, sort_order),
    INDEX idx_cook_coverage_family_size (family_size)
);

CREATE TABLE IF NOT EXISTS cook_coverage_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coverage_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    icon VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_cook_coverage_item_coverage_sort (coverage_id, sort_order),
    CONSTRAINT fk_cook_coverage_item_coverage
        FOREIGN KEY (coverage_id) REFERENCES cook_coverage(id)
        ON DELETE CASCADE
);

INSERT INTO cook_coverage (family_size, members, family_size_label, reference, sort_order, is_active)
SELECT 'SMALL', '1-2', '1-2 Members', 'Standard home cooking', 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage WHERE family_size = 'SMALL');

INSERT INTO cook_coverage (family_size, members, family_size_label, reference, sort_order, is_active)
SELECT 'MEDIUM', '3-4', '3-4 Members', 'Standard home cooking', 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage WHERE family_size = 'MEDIUM');

INSERT INTO cook_coverage (family_size, members, family_size_label, reference, sort_order, is_active)
SELECT 'LARGE', '5-6', '5-6 Members', 'Standard home cooking', 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage WHERE family_size = 'LARGE');

INSERT INTO cook_coverage (family_size, members, family_size_label, reference, sort_order, is_active)
SELECT 'XLARGE', '6-8', '6-8 Members', 'Standard home cooking', 40, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage WHERE family_size = 'XLARGE');

INSERT INTO cook_coverage_item (coverage_id, name, icon, sort_order)
SELECT id, 'Dal', 'dal', 1 FROM cook_coverage c
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage_item i WHERE i.coverage_id = c.id)
  AND c.family_size IN ('SMALL', 'MEDIUM', 'LARGE', 'XLARGE');

INSERT INTO cook_coverage_item (coverage_id, name, icon, sort_order)
SELECT id, 'Rice', 'rice', 2 FROM cook_coverage c
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage_item i WHERE i.coverage_id = c.id AND i.sort_order = 2)
  AND c.family_size IN ('SMALL', 'MEDIUM', 'LARGE', 'XLARGE');

INSERT INTO cook_coverage_item (coverage_id, name, icon, sort_order)
SELECT id, CASE WHEN family_size = 'SMALL' THEN '1 Sabzi' ELSE '2 Sabzi' END, 'sabzi', 3 FROM cook_coverage c
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage_item i WHERE i.coverage_id = c.id AND i.sort_order = 3)
  AND c.family_size IN ('SMALL', 'MEDIUM', 'LARGE', 'XLARGE');

INSERT INTO cook_coverage_item (coverage_id, name, icon, sort_order)
SELECT id,
       CASE family_size
           WHEN 'SMALL' THEN '6-8 Rotis'
           WHEN 'MEDIUM' THEN '12-15 Rotis'
           WHEN 'LARGE' THEN '18-22 Rotis'
           ELSE '24-30 Rotis'
       END,
       'roti',
       4
FROM cook_coverage c
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage_item i WHERE i.coverage_id = c.id AND i.sort_order = 4)
  AND c.family_size IN ('SMALL', 'MEDIUM', 'LARGE', 'XLARGE');

INSERT INTO cook_coverage_item (coverage_id, name, icon, sort_order)
SELECT id, 'Salad', 'salad', 5 FROM cook_coverage c
WHERE NOT EXISTS (SELECT 1 FROM cook_coverage_item i WHERE i.coverage_id = c.id AND i.sort_order = 5)
  AND c.family_size IN ('SMALL', 'MEDIUM', 'LARGE', 'XLARGE');
