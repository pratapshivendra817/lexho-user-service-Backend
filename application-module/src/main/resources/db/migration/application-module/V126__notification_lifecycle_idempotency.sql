SET @sql_add_notification_idempotency_key := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'idempotency_key'),
    'ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(160) NULL',
    'SELECT 1'
);
PREPARE stmt_add_notification_idempotency_key FROM @sql_add_notification_idempotency_key;
EXECUTE stmt_add_notification_idempotency_key;
DEALLOCATE PREPARE stmt_add_notification_idempotency_key;

SET @sql_idx_notification_idempotency_key := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'uq_notifications_idempotency_key'),
    'CREATE UNIQUE INDEX uq_notifications_idempotency_key ON notifications(idempotency_key)',
    'SELECT 1'
);
PREPARE stmt_idx_notification_idempotency_key FROM @sql_idx_notification_idempotency_key;
EXECUTE stmt_idx_notification_idempotency_key;
DEALLOCATE PREPARE stmt_idx_notification_idempotency_key;
