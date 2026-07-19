SET @db_name = DATABASE();

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.views
         WHERE table_schema = @db_name
           AND table_name = 'customer_live_location') = 0,
        'CREATE VIEW customer_live_location AS SELECT * FROM customer_current_locations',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.views
         WHERE table_schema = @db_name
           AND table_name = 'saved_addresses') = 0,
        'CREATE VIEW saved_addresses AS SELECT * FROM customer_addresses WHERE is_deleted = FALSE',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.views
         WHERE table_schema = @db_name
           AND table_name = 'service_zones') = 0,
        'CREATE VIEW service_zones AS SELECT id, city_id, name, polygon_json, active, deleted, created_at, updated_at FROM zones',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        (SELECT COUNT(*)
         FROM information_schema.views
         WHERE table_schema = @db_name
           AND table_name = 'serviceability_rules') = 0,
        'CREATE VIEW serviceability_rules AS SELECT id, name, city_id, zone_id, city, state, pincode, polygon_json, active, deleted, created_at, updated_at FROM serviceable_areas',
        'SELECT 1'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
