ALTER TABLE cook_coverage_item
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 AFTER sort_order;
