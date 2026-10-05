CREATE TABLE IF NOT EXISTS supported_languages (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    native_name VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS translation_catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    locale_code VARCHAR(10) NOT NULL,
    translation_key VARCHAR(180) NOT NULL,
    translation_value TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_translation_locale_key UNIQUE (locale_code, translation_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_translation_catalog_locale
    ON translation_catalog(locale_code);

CREATE TABLE IF NOT EXISTS partner_preferences (
    partner_id BIGINT PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DELETE FROM translation_catalog WHERE locale_code IN ('ur-IN', 'ur');
DELETE FROM partner_preferences WHERE language_code IN ('ur-IN', 'ur');
DELETE FROM supported_languages WHERE code IN ('ur-IN', 'ur');

INSERT INTO supported_languages (code, name, native_name, enabled, is_default, created_at, updated_at) VALUES
('en-IN', 'English', 'English', TRUE, TRUE, NOW(6), NOW(6)),
('mr-IN', 'Marathi', 'मराठी', TRUE, FALSE, NOW(6), NOW(6)),
('hi-IN', 'Hindi', 'हिन्दी', TRUE, FALSE, NOW(6), NOW(6)),
('gu-IN', 'Gujarati', 'ગુજરાતી', TRUE, FALSE, NOW(6), NOW(6)),
('ta-IN', 'Tamil', 'தமிழ்', TRUE, FALSE, NOW(6), NOW(6)),
('te-IN', 'Telugu', 'తెలుగు', TRUE, FALSE, NOW(6), NOW(6)),
('bn-IN', 'Bengali', 'বাংলা', TRUE, FALSE, NOW(6), NOW(6)),
('kn-IN', 'Kannada', 'ಕನ್ನಡ', TRUE, FALSE, NOW(6), NOW(6)),
('ml-IN', 'Malayalam', 'മലയാളം', TRUE, FALSE, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    native_name = VALUES(native_name),
    enabled = VALUES(enabled),
    is_default = VALUES(is_default),
    updated_at = NOW(6);

INSERT INTO translation_catalog (locale_code, translation_key, translation_value, version, created_at, updated_at) VALUES
('en-IN', 'profile.title', 'Profile', 1, NOW(6), NOW(6)),
('en-IN', 'settings.language', 'Language', 1, NOW(6), NOW(6)),
('en-IN', 'logout.button', 'Log out', 1, NOW(6), NOW(6)),
('en-IN', 'api.message.invalid.request', 'Invalid request', 1, NOW(6), NOW(6)),
('en-IN', 'api.message.language.is.required', 'language is required', 1, NOW(6), NOW(6)),
('en-IN', 'api.message.unsupported.language', 'Unsupported language', 1, NOW(6), NOW(6)),
('en-IN', 'warning.title', 'WARNING', 1, NOW(6), NOW(6)),
('en-IN', 'warning.guidance.warning', 'Review the issue and avoid repeating it on future jobs.', 1, NOW(6), NOW(6)),
('en-IN', 'warning.guidance.blocked', 'Contact support or admin before taking new jobs.', 1, NOW(6), NOW(6)),
('en-IN', 'warning.guidance.default', 'Follow admin guidance.', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.title', 'New Warning Issued', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.message', 'A new warning has been issued to your account. Tap to view the details.', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.under_review.title', 'Warning Dispute Under Review', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.under_review.message', 'Your warning dispute is under review. Tap to view the latest update.', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.approved.title', 'Warning Dispute Approved', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.approved.message', 'Your warning dispute has been approved. Tap to view the latest update.', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.rejected.title', 'Warning Dispute Rejected', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.rejected.message', 'Your warning dispute has been rejected. Tap to view the latest update.', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.updated.title', 'Warning Dispute Updated', 1, NOW(6), NOW(6)),
('en-IN', 'notification.warning.dispute.updated.message', 'Your warning dispute has been updated. Tap to view the latest update.', 1, NOW(6), NOW(6)),
('mr-IN', 'profile.title', 'प्रोफाइल', 1, NOW(6), NOW(6)),
('mr-IN', 'settings.language', 'भाषा', 1, NOW(6), NOW(6)),
('mr-IN', 'logout.button', 'लॉग आउट', 1, NOW(6), NOW(6)),
('mr-IN', 'api.message.invalid.request', 'अवैध विनंती', 1, NOW(6), NOW(6)),
('mr-IN', 'api.message.language.is.required', 'भाषा आवश्यक आहे', 1, NOW(6), NOW(6)),
('mr-IN', 'api.message.unsupported.language', 'समर्थित नसलेली भाषा', 1, NOW(6), NOW(6)),
('mr-IN', 'warning.title', 'चेतावणी', 1, NOW(6), NOW(6)),
('mr-IN', 'warning.guidance.warning', 'समस्या तपासा आणि पुढील कामांमध्ये ती पुन्हा होऊ देऊ नका.', 1, NOW(6), NOW(6)),
('mr-IN', 'warning.guidance.blocked', 'नवीन कामे घेण्यापूर्वी सपोर्ट किंवा अॅडमिनशी संपर्क साधा.', 1, NOW(6), NOW(6)),
('mr-IN', 'warning.guidance.default', 'अॅडमिनच्या सूचनांचे पालन करा.', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.title', 'नवीन चेतावणी जारी झाली', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.message', 'तुमच्या खात्यावर नवीन चेतावणी जारी झाली आहे. तपशील पाहण्यासाठी टॅप करा.', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.under_review.title', 'चेतावणी विवाद पुनरावलोकनात आहे', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.under_review.message', 'तुमचा चेतावणी विवाद पुनरावलोकनात आहे. नवीन अपडेट पाहण्यासाठी टॅप करा.', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.approved.title', 'चेतावणी विवाद मंजूर झाला', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.approved.message', 'तुमचा चेतावणी विवाद मंजूर झाला आहे. नवीन अपडेट पाहण्यासाठी टॅप करा.', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.rejected.title', 'चेतावणी विवाद नाकारला गेला', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.rejected.message', 'तुमचा चेतावणी विवाद नाकारला गेला आहे. नवीन अपडेट पाहण्यासाठी टॅप करा.', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.updated.title', 'चेतावणी विवाद अपडेट झाला', 1, NOW(6), NOW(6)),
('mr-IN', 'notification.warning.dispute.updated.message', 'तुमचा चेतावणी विवाद अपडेट झाला आहे. नवीन अपडेट पाहण्यासाठी टॅप करा.', 1, NOW(6), NOW(6)),
('hi-IN', 'profile.title', 'प्रोफाइल', 1, NOW(6), NOW(6)),
('hi-IN', 'settings.language', 'भाषा', 1, NOW(6), NOW(6)),
('hi-IN', 'logout.button', 'लॉग आउट', 1, NOW(6), NOW(6)),
('gu-IN', 'profile.title', 'પ્રોફાઇલ', 1, NOW(6), NOW(6)),
('gu-IN', 'settings.language', 'ભાષા', 1, NOW(6), NOW(6)),
('gu-IN', 'logout.button', 'લૉગ આઉટ', 1, NOW(6), NOW(6)),
('ta-IN', 'profile.title', 'சுயவிவரம்', 1, NOW(6), NOW(6)),
('ta-IN', 'settings.language', 'மொழி', 1, NOW(6), NOW(6)),
('ta-IN', 'logout.button', 'வெளியேறு', 1, NOW(6), NOW(6)),
('te-IN', 'profile.title', 'ప్రొఫైల్', 1, NOW(6), NOW(6)),
('te-IN', 'settings.language', 'భాష', 1, NOW(6), NOW(6)),
('te-IN', 'logout.button', 'లాగ్ అవుట్', 1, NOW(6), NOW(6)),
('bn-IN', 'profile.title', 'প্রোফাইল', 1, NOW(6), NOW(6)),
('bn-IN', 'settings.language', 'ভাষা', 1, NOW(6), NOW(6)),
('bn-IN', 'logout.button', 'লগ আউট', 1, NOW(6), NOW(6)),
('kn-IN', 'profile.title', 'ಪ್ರೊಫೈಲ್', 1, NOW(6), NOW(6)),
('kn-IN', 'settings.language', 'ಭಾಷೆ', 1, NOW(6), NOW(6)),
('kn-IN', 'logout.button', 'ಲಾಗ್ ಔಟ್', 1, NOW(6), NOW(6)),
('ml-IN', 'profile.title', 'പ്രൊഫൈൽ', 1, NOW(6), NOW(6)),
('ml-IN', 'settings.language', 'ഭാഷ', 1, NOW(6), NOW(6)),
('ml-IN', 'logout.button', 'ലോഗ് ഔട്ട്', 1, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    translation_value = VALUES(translation_value),
    version = VALUES(version),
    updated_at = NOW(6);
