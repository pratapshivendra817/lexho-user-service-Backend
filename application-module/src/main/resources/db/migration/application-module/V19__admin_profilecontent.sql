CREATE TABLE IF NOT EXISTS admin_profilecontent (
    id BIGINT PRIMARY KEY,
    about_us TEXT NOT NULL,
    terms_of_service TEXT NOT NULL,
    privacy_policy TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO admin_profilecontent (id, about_us, terms_of_service, privacy_policy)
SELECT 1, '', '', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_profilecontent WHERE id = 1);
