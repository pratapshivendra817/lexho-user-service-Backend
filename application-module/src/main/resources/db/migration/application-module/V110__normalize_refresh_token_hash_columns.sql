UPDATE lexho_refresh_tokens
SET token = LOWER(SHA2(TRIM(token), 256))
WHERE token IS NOT NULL
  AND token NOT REGEXP '^[0-9a-f]{64}$';

UPDATE auth_refresh_tokens
SET token = LOWER(SHA2(TRIM(token), 256))
WHERE token IS NOT NULL
  AND token NOT REGEXP '^[0-9a-f]{64}$';

ALTER TABLE lexho_refresh_tokens
    MODIFY COLUMN token VARCHAR(64) NOT NULL;

ALTER TABLE auth_refresh_tokens
    MODIFY COLUMN token VARCHAR(64) NOT NULL;
