CREATE TABLE IF NOT EXISTS partner_service (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    sub_service_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_partner_service_partner FOREIGN KEY (partner_id) REFERENCES partners(id),
    CONSTRAINT fk_partner_service_sub_service FOREIGN KEY (sub_service_id) REFERENCES services(id),
    UNIQUE KEY uq_partner_service_skill (partner_id, sub_service_id),
    INDEX idx_partner_service_partner (partner_id),
    INDEX idx_partner_service_sub_service (sub_service_id)
);
