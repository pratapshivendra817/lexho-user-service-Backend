ALTER TABLE sections
    ADD COLUMN subtitle VARCHAR(255) NULL AFTER title,
    ADD COLUMN priority INT NOT NULL DEFAULT 0 AFTER icon;

UPDATE sections
SET priority = CASE type
    WHEN 'STATS' THEN 0
    WHEN 'TRUST' THEN 1
    WHEN 'TAGLINE' THEN 2
    WHEN 'HIGHLIGHTS' THEN 3
    ELSE 99
END
WHERE priority = 0;

ALTER TABLE sections
    ADD INDEX idx_sections_priority (priority, created_at, id);

ALTER TABLE section_items
    ADD COLUMN subtitle VARCHAR(255) NULL AFTER title;
