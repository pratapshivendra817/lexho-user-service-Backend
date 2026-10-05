DROP PROCEDURE IF EXISTS align_customer_location_uuid_storage;

DELIMITER $$

CREATE PROCEDURE align_customer_location_uuid_storage()
BEGIN
    DECLARE customer_addresses_id_type VARCHAR(64);
    DECLARE customer_current_locations_id_type VARCHAR(64);

    SELECT DATA_TYPE
    INTO customer_addresses_id_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customer_addresses'
      AND COLUMN_NAME = 'id'
    LIMIT 1;

    IF customer_addresses_id_type IS NOT NULL AND customer_addresses_id_type <> 'binary' THEN
        ALTER TABLE customer_addresses ADD COLUMN id_bin BINARY(16) NULL FIRST;
        UPDATE customer_addresses
        SET id_bin = UNHEX(REPLACE(id, '-', ''))
        WHERE id IS NOT NULL;
        ALTER TABLE customer_addresses DROP PRIMARY KEY;
        ALTER TABLE customer_addresses DROP COLUMN id;
        ALTER TABLE customer_addresses CHANGE COLUMN id_bin id BINARY(16) NOT NULL;
        ALTER TABLE customer_addresses ADD PRIMARY KEY (id);
    END IF;

    SELECT DATA_TYPE
    INTO customer_current_locations_id_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customer_current_locations'
      AND COLUMN_NAME = 'id'
    LIMIT 1;

    IF customer_current_locations_id_type IS NOT NULL AND customer_current_locations_id_type <> 'binary' THEN
        ALTER TABLE customer_current_locations ADD COLUMN id_bin BINARY(16) NULL FIRST;
        UPDATE customer_current_locations
        SET id_bin = UNHEX(REPLACE(id, '-', ''))
        WHERE id IS NOT NULL;
        ALTER TABLE customer_current_locations DROP PRIMARY KEY;
        ALTER TABLE customer_current_locations DROP COLUMN id;
        ALTER TABLE customer_current_locations CHANGE COLUMN id_bin id BINARY(16) NOT NULL;
        ALTER TABLE customer_current_locations ADD PRIMARY KEY (id);
    END IF;
END$$

DELIMITER ;

CALL align_customer_location_uuid_storage();

DROP PROCEDURE IF EXISTS align_customer_location_uuid_storage;
