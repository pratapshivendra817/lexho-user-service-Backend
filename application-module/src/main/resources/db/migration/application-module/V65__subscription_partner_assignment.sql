CREATE TABLE IF NOT EXISTS subscription_partner_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    replacement_partner_id BIGINT NULL,
    replacement_reason VARCHAR(500) NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replaced_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subscription_partner_assignment_subscription (subscription_id),
    INDEX idx_subscription_partner_assignment_partner (partner_id),
    CONSTRAINT fk_subscription_partner_assignment_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscription_master(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscription_visit_partner_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    visit_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subscription_visit_partner_assignment_visit (visit_id),
    INDEX idx_subscription_visit_partner_assignment_subscription (subscription_id),
    INDEX idx_subscription_visit_partner_assignment_partner (partner_id),
    CONSTRAINT fk_subscription_visit_partner_assignment_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscription_master(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_subscription_visit_partner_assignment_visit
        FOREIGN KEY (visit_id) REFERENCES subscription_visit(id)
        ON DELETE CASCADE
);
