SET @has_notifications_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'notifications'
);

SET @sql_create_notifications := IF(
    @has_notifications_table = 0,
    'CREATE TABLE notifications (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        user_type VARCHAR(20) NOT NULL DEFAULT ''CUSTOMER'',
        title VARCHAR(255) NOT NULL,
        message VARCHAR(2000) NOT NULL,
        notification_type VARCHAR(20) NOT NULL DEFAULT ''SYSTEM'',
        channel_type VARCHAR(20) NOT NULL DEFAULT ''IN_APP'',
        priority VARCHAR(20) NOT NULL DEFAULT ''MEDIUM'',
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
        reference_id BIGINT NULL,
        reference_type VARCHAR(60) NULL,
        redirect_type VARCHAR(60) NULL,
        redirect_value VARCHAR(500) NULL,
        scheduled_time DATETIME NULL,
        expiry_time DATETIME NULL,
        sent_at DATETIME NULL,
        read_at DATETIME NULL,
        clicked_at DATETIME NULL,
        retry_count INT NOT NULL DEFAULT 0,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        deleted BOOLEAN NOT NULL DEFAULT FALSE
    )',
    'SELECT 1'
);
PREPARE stmt_create_notifications FROM @sql_create_notifications;
EXECUTE stmt_create_notifications;
DEALLOCATE PREPARE stmt_create_notifications;

SET @sql_add_user_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'user_type'),
    'ALTER TABLE notifications ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT ''CUSTOMER''',
    'SELECT 1'
);
PREPARE stmt_add_user_type FROM @sql_add_user_type;
EXECUTE stmt_add_user_type;
DEALLOCATE PREPARE stmt_add_user_type;

SET @sql_add_notification_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'notification_type'),
    'ALTER TABLE notifications ADD COLUMN notification_type VARCHAR(20) NOT NULL DEFAULT ''SYSTEM''',
    'SELECT 1'
);
PREPARE stmt_add_notification_type FROM @sql_add_notification_type;
EXECUTE stmt_add_notification_type;
DEALLOCATE PREPARE stmt_add_notification_type;

SET @sql_copy_type := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'type')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'notification_type'),
    'UPDATE notifications SET notification_type = UPPER(type) WHERE (notification_type IS NULL OR notification_type = '''') AND type IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_copy_type FROM @sql_copy_type;
EXECUTE stmt_copy_type;
DEALLOCATE PREPARE stmt_copy_type;

SET @sql_add_channel_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'channel_type'),
    'ALTER TABLE notifications ADD COLUMN channel_type VARCHAR(20) NOT NULL DEFAULT ''IN_APP''',
    'SELECT 1'
);
PREPARE stmt_add_channel_type FROM @sql_add_channel_type;
EXECUTE stmt_add_channel_type;
DEALLOCATE PREPARE stmt_add_channel_type;

SET @sql_add_priority := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'priority'),
    'ALTER TABLE notifications ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT ''MEDIUM''',
    'SELECT 1'
);
PREPARE stmt_add_priority FROM @sql_add_priority;
EXECUTE stmt_add_priority;
DEALLOCATE PREPARE stmt_add_priority;

SET @sql_add_status := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'status'),
    'ALTER TABLE notifications ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING''',
    'SELECT 1'
);
PREPARE stmt_add_status FROM @sql_add_status;
EXECUTE stmt_add_status;
DEALLOCATE PREPARE stmt_add_status;

SET @sql_sync_read_status := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'is_read')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'status'),
    'UPDATE notifications SET status = CASE WHEN is_read = TRUE THEN ''READ'' ELSE ''SENT'' END WHERE status IS NULL OR status = ''''',
    'SELECT 1'
);
PREPARE stmt_sync_read_status FROM @sql_sync_read_status;
EXECUTE stmt_sync_read_status;
DEALLOCATE PREPARE stmt_sync_read_status;

SET @sql_add_reference_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'reference_type'),
    'ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(60) NULL',
    'SELECT 1'
);
PREPARE stmt_add_reference_type FROM @sql_add_reference_type;
EXECUTE stmt_add_reference_type;
DEALLOCATE PREPARE stmt_add_reference_type;

SET @sql_add_redirect_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'redirect_type'),
    'ALTER TABLE notifications ADD COLUMN redirect_type VARCHAR(60) NULL',
    'SELECT 1'
);
PREPARE stmt_add_redirect_type FROM @sql_add_redirect_type;
EXECUTE stmt_add_redirect_type;
DEALLOCATE PREPARE stmt_add_redirect_type;

SET @sql_add_redirect_value := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'redirect_value'),
    'ALTER TABLE notifications ADD COLUMN redirect_value VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt_add_redirect_value FROM @sql_add_redirect_value;
EXECUTE stmt_add_redirect_value;
DEALLOCATE PREPARE stmt_add_redirect_value;

SET @sql_add_scheduled_time := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'scheduled_time'),
    'ALTER TABLE notifications ADD COLUMN scheduled_time DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_scheduled_time FROM @sql_add_scheduled_time;
EXECUTE stmt_add_scheduled_time;
DEALLOCATE PREPARE stmt_add_scheduled_time;

SET @sql_add_expiry_time := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'expiry_time'),
    'ALTER TABLE notifications ADD COLUMN expiry_time DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_expiry_time FROM @sql_add_expiry_time;
EXECUTE stmt_add_expiry_time;
DEALLOCATE PREPARE stmt_add_expiry_time;

SET @sql_add_sent_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'sent_at'),
    'ALTER TABLE notifications ADD COLUMN sent_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_sent_at FROM @sql_add_sent_at;
EXECUTE stmt_add_sent_at;
DEALLOCATE PREPARE stmt_add_sent_at;

SET @sql_add_read_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'read_at'),
    'ALTER TABLE notifications ADD COLUMN read_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_read_at FROM @sql_add_read_at;
EXECUTE stmt_add_read_at;
DEALLOCATE PREPARE stmt_add_read_at;

SET @sql_add_clicked_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'clicked_at'),
    'ALTER TABLE notifications ADD COLUMN clicked_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_add_clicked_at FROM @sql_add_clicked_at;
EXECUTE stmt_add_clicked_at;
DEALLOCATE PREPARE stmt_add_clicked_at;

SET @sql_add_retry_count := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'retry_count'),
    'ALTER TABLE notifications ADD COLUMN retry_count INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_add_retry_count FROM @sql_add_retry_count;
EXECUTE stmt_add_retry_count;
DEALLOCATE PREPARE stmt_add_retry_count;

SET @sql_add_updated_at := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'updated_at'),
    'ALTER TABLE notifications ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt_add_updated_at FROM @sql_add_updated_at;
EXECUTE stmt_add_updated_at;
DEALLOCATE PREPARE stmt_add_updated_at;

SET @sql_add_deleted := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'deleted'),
    'ALTER TABLE notifications ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_deleted FROM @sql_add_deleted;
EXECUTE stmt_add_deleted;
DEALLOCATE PREPARE stmt_add_deleted;

CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(120) NOT NULL UNIQUE,
    notification_type VARCHAR(20) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    message_template VARCHAR(2000) NOT NULL,
    language VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    promotional_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    booking_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

SET @sql_idx_notifications_user := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_user_id'),
    'CREATE INDEX idx_notifications_user_id ON notifications(user_id)',
    'SELECT 1'
);
PREPARE stmt_idx_notifications_user FROM @sql_idx_notifications_user;
EXECUTE stmt_idx_notifications_user;
DEALLOCATE PREPARE stmt_idx_notifications_user;

SET @sql_idx_notifications_status := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_status'),
    'CREATE INDEX idx_notifications_status ON notifications(status)',
    'SELECT 1'
);
PREPARE stmt_idx_notifications_status FROM @sql_idx_notifications_status;
EXECUTE stmt_idx_notifications_status;
DEALLOCATE PREPARE stmt_idx_notifications_status;

SET @sql_idx_notifications_type := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_type'),
    'CREATE INDEX idx_notifications_type ON notifications(notification_type)',
    'SELECT 1'
);
PREPARE stmt_idx_notifications_type FROM @sql_idx_notifications_type;
EXECUTE stmt_idx_notifications_type;
DEALLOCATE PREPARE stmt_idx_notifications_type;

SET @sql_idx_notifications_created := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_created_at'),
    'CREATE INDEX idx_notifications_created_at ON notifications(created_at)',
    'SELECT 1'
);
PREPARE stmt_idx_notifications_created FROM @sql_idx_notifications_created;
EXECUTE stmt_idx_notifications_created;
DEALLOCATE PREPARE stmt_idx_notifications_created;
