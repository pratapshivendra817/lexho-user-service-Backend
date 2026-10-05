SET @sql_add_category_commission := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'admin_categories' AND column_name = 'commission_percent'),
    'ALTER TABLE admin_categories ADD COLUMN commission_percent DECIMAL(5,2) NULL',
    'SELECT 1'
);
PREPARE stmt_add_category_commission FROM @sql_add_category_commission;
EXECUTE stmt_add_category_commission;
DEALLOCATE PREPARE stmt_add_category_commission;

SET @sql_add_category_review_enabled := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'admin_categories' AND column_name = 'review_enabled'),
    'ALTER TABLE admin_categories ADD COLUMN review_enabled BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1'
);
PREPARE stmt_add_category_review_enabled FROM @sql_add_category_review_enabled;
EXECUTE stmt_add_category_review_enabled;
DEALLOCATE PREPARE stmt_add_category_review_enabled;

SET @sql_add_service_commission := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'services' AND column_name = 'commission_percent'),
    'ALTER TABLE services ADD COLUMN commission_percent DECIMAL(5,2) NULL',
    'SELECT 1'
);
PREPARE stmt_add_service_commission FROM @sql_add_service_commission;
EXECUTE stmt_add_service_commission;
DEALLOCATE PREPARE stmt_add_service_commission;

SET @sql_add_service_review_enabled := IF(
    NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'services' AND column_name = 'review_enabled'),
    'ALTER TABLE services ADD COLUMN review_enabled BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1'
);
PREPARE stmt_add_service_review_enabled FROM @sql_add_service_review_enabled;
EXECUTE stmt_add_service_review_enabled;
DEALLOCATE PREPARE stmt_add_service_review_enabled;

INSERT INTO notification_templates (template_name, notification_type, title_template, message_template, language, active)
SELECT 'SERVICE_COMPLETED_CUSTOMER', 'SERVICE_COMPLETED', 'Service Completed',
       'Your service has been completed successfully.', 'en', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates
    WHERE template_name = 'SERVICE_COMPLETED_CUSTOMER' AND language = 'en'
);

INSERT INTO notification_templates (template_name, notification_type, title_template, message_template, language, active)
SELECT 'SERVICE_COMPLETED_PARTNER', 'SERVICE_COMPLETED', 'Service Completed',
       'Service completed. Your earnings have been updated: INR {{earningAmount}}.', 'en', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates
    WHERE template_name = 'SERVICE_COMPLETED_PARTNER' AND language = 'en'
);
