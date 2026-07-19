CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NULL,
    phone_number VARCHAR(20) NOT NULL,
    gender VARCHAR(10) NULL,
    profile_image_url VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_customers_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER') OR gender IS NULL),
    CONSTRAINT uk_customers_phone_number UNIQUE (phone_number),
    CONSTRAINT uk_customers_email UNIQUE (email)
);
