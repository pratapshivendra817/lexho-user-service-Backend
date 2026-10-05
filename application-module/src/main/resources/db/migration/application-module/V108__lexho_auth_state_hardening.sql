CREATE TABLE IF NOT EXISTS lexho_refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    CONSTRAINT uq_lexho_refresh_tokens_token UNIQUE (token),
    INDEX idx_lexho_refresh_tokens_user_id (user_id),
    INDEX idx_lexho_refresh_tokens_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_auth_refresh_tokens_token UNIQUE (token),
    INDEX idx_auth_refresh_tokens_user_id (user_id),
    INDEX idx_auth_refresh_tokens_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS lexho_login_otps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mobile VARCHAR(15) NOT NULL,
    role VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_lexho_login_otps_lookup (mobile, role, consumed_at, created_at),
    INDEX idx_lexho_login_otps_expires_at (expires_at)
);

UPDATE lexho_refresh_tokens
SET token = LOWER(SHA2(TRIM(token), 256))
WHERE token IS NOT NULL
  AND token NOT REGEXP '^[0-9a-f]{64}$';

UPDATE auth_refresh_tokens
SET token = LOWER(SHA2(TRIM(token), 256))
WHERE token IS NOT NULL
  AND token NOT REGEXP '^[0-9a-f]{64}$';
