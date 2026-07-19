CREATE TABLE IF NOT EXISTS location_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    pincode VARCHAR(16) NULL,
    source VARCHAR(32) NULL,
    mock_location BOOLEAN NOT NULL DEFAULT FALSE,
    suspicious_jump BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    event_type VARCHAR(64) NOT NULL,
    event_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_audit_logs_customer_id ON location_audit_logs (customer_id);
CREATE INDEX idx_location_audit_logs_created_at ON location_audit_logs (created_at);
