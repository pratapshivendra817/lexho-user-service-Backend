CREATE TABLE IF NOT EXISTS lexho_user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mobile VARCHAR(15) NOT NULL,
    name VARCHAR(120) NULL,
    email VARCHAR(150) NULL,
    profile_image VARCHAR(512) NULL,
    gender VARCHAR(20) NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uq_lexho_user_profiles_mobile UNIQUE (mobile)
);

CREATE TABLE IF NOT EXISTS lexho_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lexho_fav_user (user_id),
    INDEX idx_lexho_fav_service (service_id)
);

CREATE TABLE IF NOT EXISTS lexho_referral_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lexho_referral_codes_code UNIQUE (code),
    INDEX idx_lexho_ref_user (user_id),
    INDEX idx_lexho_ref_code (code)
);

CREATE TABLE IF NOT EXISTS lexho_referral_uses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lexho_refuse_user (user_id),
    INDEX idx_lexho_refuse_code (code)
);
