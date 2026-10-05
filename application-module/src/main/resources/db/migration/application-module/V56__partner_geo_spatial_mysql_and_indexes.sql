SET @db_name = DATABASE();

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'geohash') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN geohash VARCHAR(20) NULL AFTER source',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.columns
         WHERE table_schema = @db_name
           AND table_name = 'current_partner_locations'
           AND column_name = 'geo_point') = 0,
        'ALTER TABLE current_partner_locations ADD COLUMN geo_point POINT SRID 4326 NULL AFTER longitude',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE current_partner_locations
SET geohash = COALESCE(geohash, ''),
    geo_point = ST_SRID(POINT(longitude, latitude), 4326)
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND index_name = 'idx_current_partner_locations_geo_point'
);
SET @geo_point_not_null := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND column_name = 'geo_point'
      AND is_nullable = 'NO'
);
SET @null_geo_points := (
    SELECT COUNT(1)
    FROM current_partner_locations
    WHERE geo_point IS NULL
);
SET @ddl_spatial := IF(@idx_exists = 0 AND @null_geo_points = 0 AND @geo_point_not_null = 1,
                       'CREATE SPATIAL INDEX idx_current_partner_locations_geo_point ON current_partner_locations (geo_point)',
                       'SELECT 1');
PREPARE stmt_spatial FROM @ddl_spatial;
EXECUTE stmt_spatial;
DEALLOCATE PREPARE stmt_spatial;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
      AND index_name = 'idx_current_partner_locations_geohash'
);
SET @ddl_idx := IF(@idx_exists = 0,
                   'CREATE INDEX idx_current_partner_locations_geohash ON current_partner_locations (geohash)',
                   'SELECT 1');
PREPARE stmt_idx FROM @ddl_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'current_partner_locations'
    GROUP BY index_name
    HAVING COUNT(*) = 3
       AND MAX(CASE WHEN seq_in_index = 1 AND column_name = 'partner_id' THEN 1 ELSE 0 END) = 1
       AND MAX(CASE WHEN seq_in_index = 2 AND column_name = 'deleted' THEN 1 ELSE 0 END) = 1
       AND MAX(CASE WHEN seq_in_index = 3 AND column_name = 'updated_at' THEN 1 ELSE 0 END) = 1
);
SET @ddl_idx := IF(@idx_exists = 0,
                   'CREATE INDEX idx_current_partner_locations_partner_deleted_updated ON current_partner_locations (partner_id, deleted, updated_at)',
                   'SELECT 1');
PREPARE stmt_idx FROM @ddl_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
