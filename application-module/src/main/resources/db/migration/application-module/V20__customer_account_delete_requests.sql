CREATE TABLE IF NOT EXISTS customer_account_delete_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cadr_user_id (user_id),
    INDEX idx_cadr_resolved (resolved)
);
