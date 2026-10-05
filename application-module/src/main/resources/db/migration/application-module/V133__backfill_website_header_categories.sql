INSERT INTO website_category_config (
    category_id,
    show_on_website,
    display_order,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    c.id,
    b'1',
    COALESCE(c.display_order, 999),
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL,
    NULL
FROM admin_categories c
LEFT JOIN website_category_config wcc ON wcc.category_id = c.id
WHERE c.is_active = b'1'
  AND c.is_deleted = b'0'
  AND wcc.id IS NULL;
