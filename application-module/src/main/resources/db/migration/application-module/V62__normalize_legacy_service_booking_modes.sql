-- Normalize legacy service_type labels into booking mode tokens used by customer API mapping.
-- Older admin data used labels such as Home_cleaning/cook instead of FIXED/HOURLY.

UPDATE services
SET service_type = 'FIXED,HOURLY'
WHERE COALESCE(is_deleted, FALSE) = FALSE
  AND (
        LOWER(TRIM(COALESCE(service_type, ''))) IN ('home_cleaning', 'home cleaning', 'cleaning', 'cook', 'cooking')
        OR LOWER(COALESCE(category_name, '')) LIKE '%clean%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%cook%'
      );

UPDATE services
SET service_type = 'FIXED,HOURLY'
WHERE COALESCE(is_deleted, FALSE) = FALSE
  AND (
        LOWER(TRIM(COALESCE(service_type, ''))) IN ('repair', 'repairs')
        OR LOWER(COALESCE(category_name, '')) LIKE '%repair%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%electric%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%plumb%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%carpenter%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%ac%'
        OR LOWER(COALESCE(category_name, '')) LIKE '%appliance%'
      );
