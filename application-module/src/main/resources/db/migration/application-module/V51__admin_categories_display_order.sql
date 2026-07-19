ALTER TABLE admin_categories
    ADD COLUMN display_order INTEGER;

UPDATE admin_categories
SET display_order = 999
WHERE display_order IS NULL;

ALTER TABLE admin_categories
    MODIFY COLUMN display_order INTEGER NOT NULL DEFAULT 999;
