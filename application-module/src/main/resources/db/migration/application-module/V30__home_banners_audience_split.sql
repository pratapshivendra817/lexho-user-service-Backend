ALTER TABLE home_banners
    ADD COLUMN audience VARCHAR(20) NULL AFTER banner_type;

UPDATE home_banners
SET audience = 'BOTH'
WHERE audience IS NULL OR TRIM(audience) = '';

ALTER TABLE home_banners
    MODIFY COLUMN audience VARCHAR(20) NOT NULL;

CREATE INDEX idx_home_banners_active_audience_priority
    ON home_banners (is_active, audience, priority);
