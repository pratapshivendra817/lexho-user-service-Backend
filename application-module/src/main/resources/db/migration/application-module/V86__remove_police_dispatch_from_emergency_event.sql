SET @drop_notify_police = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'emergency_event'
          AND column_name = 'notify_police'
    ),
    'ALTER TABLE emergency_event DROP COLUMN notify_police',
    'SELECT 1'
);
PREPARE stmt FROM @drop_notify_police;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_police_dispatch_status = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'emergency_event'
          AND column_name = 'police_dispatch_status'
    ),
    'ALTER TABLE emergency_event DROP COLUMN police_dispatch_status',
    'SELECT 1'
);
PREPARE stmt FROM @drop_police_dispatch_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_police_dispatch_reference = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'emergency_event'
          AND column_name = 'police_dispatch_reference'
    ),
    'ALTER TABLE emergency_event DROP COLUMN police_dispatch_reference',
    'SELECT 1'
);
PREPARE stmt FROM @drop_police_dispatch_reference;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
