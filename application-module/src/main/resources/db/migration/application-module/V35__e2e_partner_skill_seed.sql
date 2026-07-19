UPDATE partner
SET sub_category_id = 1
WHERE id = 4;

INSERT INTO partner_service (partner_id, sub_service_id, created_at)
SELECT 4, 1, NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM partner_service
    WHERE partner_id = 4
      AND sub_service_id = 1
);
