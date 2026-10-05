/*
 * partners is the canonical identity and status source. The legacy partner
 * table remains a marketplace projection while existing foreign-key consumers
 * are migrated. Rating, price, image and review totals are marketplace-owned
 * and are intentionally not overwritten.
 */
INSERT INTO partner (
    id, name, category_id, rating, total_reviews, available, experience,
    image, verification_status
)
SELECT
    p.id,
    COALESCE(NULLIF(p.full_name, ''), CONCAT('Partner-', p.id)),
    p.service_category_id,
    0.0,
    COALESCE(p.total_reviews, 0),
    CASE
        WHEN COALESCE(p.deleted, 0) = 1 THEN 0
        WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) <> 'APPROVED' THEN 0
        WHEN COALESCE(p.is_online, 0) = 0 THEN 0
        ELSE 1
    END,
    COALESCE(p.experience_years, 0),
    NULL,
    CASE
        WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'BLOCKED' THEN 'BLOCKED'
        WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'REJECTED' THEN 'REJECTED'
        WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'APPROVED' THEN 'APPROVED'
        ELSE 'PENDING'
    END
FROM partners p
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category_id = VALUES(category_id),
    available = VALUES(available),
    experience = VALUES(experience),
    verification_status = VALUES(verification_status);
