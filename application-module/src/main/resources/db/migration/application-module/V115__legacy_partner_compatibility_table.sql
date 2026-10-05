CREATE TABLE IF NOT EXISTS partner (
    id BIGINT NOT NULL,
    name VARCHAR(120) NULL,
    category_id BIGINT NULL,
    sub_category_id BIGINT NULL,
    rating DOUBLE NULL,
    total_reviews BIGINT NULL DEFAULT 0,
    price DOUBLE NULL,
    available BIT(1) NOT NULL DEFAULT b'0',
    experience INT NULL,
    image VARCHAR(1000) NULL,
    verification_status VARCHAR(30) NULL,
    PRIMARY KEY (id),
    KEY idx_partner_category_available (category_id, available),
    KEY idx_partner_sub_category_available (sub_category_id, available),
    KEY idx_partner_verification_status (verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO partner (
    id,
    name,
    category_id,
    rating,
    total_reviews,
    available,
    experience,
    image,
    verification_status
)
SELECT
    incoming.id,
    incoming.name,
    incoming.category_id,
    incoming.rating,
    incoming.total_reviews,
    incoming.available,
    incoming.experience,
    incoming.image,
    incoming.verification_status
FROM (
    SELECT
        p.id AS id,
        COALESCE(NULLIF(p.full_name, ''), CONCAT('Partner-', p.id)) AS name,
        p.service_category_id AS category_id,
        0.0 AS rating,
        COALESCE(p.total_reviews, 0) AS total_reviews,
        CASE
            WHEN COALESCE(p.deleted, b'0') = b'1' THEN b'0'
            WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'BLOCKED' THEN b'0'
            WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'REJECTED' THEN b'0'
            ELSE b'1'
        END AS available,
        p.experience_years AS experience,
        NULL AS image,
        CASE
            WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'APPROVED' THEN 'APPROVED'
            WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'BLOCKED' THEN 'BLOCKED'
            WHEN UPPER(COALESCE(p.onboarding_status, 'PENDING')) = 'REJECTED' THEN 'REJECTED'
            ELSE 'PENDING'
        END AS verification_status
    FROM partners p
) AS incoming
ON DUPLICATE KEY UPDATE
    name = incoming.name,
    category_id = incoming.category_id,
    rating = incoming.rating,
    total_reviews = incoming.total_reviews,
    available = incoming.available,
    experience = incoming.experience,
    image = incoming.image,
    verification_status = incoming.verification_status;
