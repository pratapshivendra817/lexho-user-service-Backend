-- Backfill booking modes for existing admin service catalog rows.
-- Customer APIs derive bookingModes from services.service_type.
-- Existing non-FIXED values are preserved to avoid overwriting manually configured data.

UPDATE services
SET service_type = 'FIXED,HOURLY'
WHERE (service_type IS NULL OR TRIM(service_type) = '' OR UPPER(TRIM(service_type)) = 'FIXED')
  AND COALESCE(is_deleted, FALSE) = FALSE
  AND (
        LOWER(COALESCE(category_name, '')) LIKE '%clean%'
        OR LOWER(COALESCE(name, '')) LIKE '%clean%'
        OR LOWER(COALESCE(name, '')) LIKE '%maid%'
        OR LOWER(COALESCE(name, '')) LIKE '%house%'
        OR LOWER(COALESCE(name, '')) LIKE '%home%'
      );

UPDATE services
SET service_type = 'FIXED,HOURLY'
WHERE (service_type IS NULL OR TRIM(service_type) = '' OR UPPER(TRIM(service_type)) = 'FIXED')
  AND COALESCE(is_deleted, FALSE) = FALSE
  AND (
        LOWER(COALESCE(category_name, '')) LIKE '%cook%'
        OR LOWER(COALESCE(name, '')) LIKE '%cook%'
        OR LOWER(COALESCE(name, '')) LIKE '%chef%'
        OR LOWER(COALESCE(name, '')) LIKE '%meal%'
        OR LOWER(COALESCE(name, '')) LIKE '%tiffin%'
      );

UPDATE services
SET service_type = 'FIXED,HOURLY'
WHERE (service_type IS NULL OR TRIM(service_type) = '' OR UPPER(TRIM(service_type)) = 'FIXED')
  AND COALESCE(is_deleted, FALSE) = FALSE
  AND (
        LOWER(COALESCE(category_name, '')) LIKE '%repair%'
        OR LOWER(COALESCE(name, '')) LIKE '%repair%'
        OR LOWER(COALESCE(name, '')) LIKE '%electric%'
        OR LOWER(COALESCE(name, '')) LIKE '%plumb%'
        OR LOWER(COALESCE(name, '')) LIKE '%carpenter%'
        OR LOWER(COALESCE(name, '')) LIKE '%ac%'
        OR LOWER(COALESCE(name, '')) LIKE '%appliance%'
      );

UPDATE services
SET service_type = 'FIXED'
WHERE (service_type IS NULL OR TRIM(service_type) = '')
  AND COALESCE(is_deleted, FALSE) = FALSE;
