SET @db_name = DATABASE();

SET @stmt = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'partner_leaves' AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE partner_leaves ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''REQUESTED''',
    'SELECT 1'
);
PREPARE partner_leave_status_add FROM @stmt;
EXECUTE partner_leave_status_add;
DEALLOCATE PREPARE partner_leave_status_add;

UPDATE partner_leaves
SET status = CASE
    WHEN status IS NULL OR status = '' THEN
        CASE
            WHEN to_date < CURDATE() THEN 'COMPLETED'
            WHEN from_date > CURDATE() THEN 'SCHEDULED'
            ELSE 'ACTIVE'
        END
    ELSE status
END;
