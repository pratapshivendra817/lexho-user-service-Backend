CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(1000) NOT NULL,
    description VARCHAR(255) NULL,
    updated_by VARCHAR(80) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO app_settings(setting_key, setting_value, description, updated_by)
SELECT 'dispatch.radius.km', '15', 'Dispatch radius in km', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'dispatch.radius.km');

INSERT INTO app_settings(setting_key, setting_value, description, updated_by)
SELECT 'partner.service.radius.km', '15', 'Partner service radius in km', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'partner.service.radius.km');

