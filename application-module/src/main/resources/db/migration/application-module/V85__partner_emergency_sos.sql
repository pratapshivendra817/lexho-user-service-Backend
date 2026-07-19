CREATE TABLE emergency_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    incident_code VARCHAR(50) NOT NULL,
    partner_id BIGINT NOT NULL,
    partner_name VARCHAR(120) NOT NULL,
    partner_mobile_number VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    trigger_message VARCHAR(500) NULL,
    admin_alert_sent BIT NOT NULL DEFAULT b'0',
    notified_contacts_count INT NOT NULL DEFAULT 0,
    last_latitude DECIMAL(10,7) NULL,
    last_longitude DECIMAL(10,7) NULL,
    last_accuracy_meters DECIMAL(8,2) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_emergency_event_incident_code UNIQUE (incident_code)
);

CREATE INDEX idx_emergency_event_partner_created ON emergency_event (partner_id, created_at);

CREATE TABLE emergency_contact (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    relationship VARCHAR(80) NULL,
    email VARCHAR(150) NULL,
    is_primary_contact BIT NOT NULL DEFAULT b'0',
    is_active BIT NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_emergency_contact_partner_active ON emergency_contact (partner_id, is_active);

CREATE TABLE emergency_location (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    accuracy_meters DECIMAL(8,2) NULL,
    address VARCHAR(500) NULL,
    source VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_emergency_location_event
        FOREIGN KEY (event_id) REFERENCES emergency_event (id)
);

CREATE INDEX idx_emergency_location_event_created ON emergency_location (event_id, created_at);
