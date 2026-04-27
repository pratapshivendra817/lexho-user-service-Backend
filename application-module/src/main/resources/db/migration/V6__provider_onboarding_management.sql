CREATE TABLE IF NOT EXISTS providers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_code VARCHAR(20) UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    gender VARCHAR(20),
    date_of_birth DATE,
    current_city VARCHAR(100),
    current_state VARCHAR(100),
    current_full_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    full_address VARCHAR(500),
    service_category_id BIGINT NOT NULL,
    experience_years INT NOT NULL DEFAULT 0,
    account_number VARCHAR(64) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    kyc_failed_reason VARCHAR(500),
    onboarding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    blocked_reason VARCHAR(500),
    user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_providers_service_category FOREIGN KEY (service_category_id) REFERENCES service_category(id),
    CONSTRAINT fk_providers_user FOREIGN KEY (user_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS provider_service_areas (
    provider_id BIGINT NOT NULL,
    service_area VARCHAR(100) NOT NULL,
    CONSTRAINT fk_provider_service_areas_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE TABLE IF NOT EXISTS provider_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    file_image VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_provider_documents_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_providers_onboarding_status ON providers(onboarding_status);
CREATE INDEX idx_providers_kyc_status ON providers(kyc_status);
CREATE INDEX idx_providers_city ON providers(city);
CREATE INDEX idx_providers_service_category ON providers(service_category_id);
CREATE INDEX idx_providers_phone_number ON providers(phone_number);
CREATE INDEX idx_provider_documents_provider ON provider_documents(provider_id);
CREATE UNIQUE INDEX uq_provider_documents_provider_type ON provider_documents(provider_id, document_type);

