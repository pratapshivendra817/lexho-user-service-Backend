CREATE TABLE IF NOT EXISTS call_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    caller_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    answer_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    duration BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_call_session_status
        CHECK (status IN ('INITIATED', 'RINGING', 'CONNECTED', 'REJECTED', 'MISSED', 'BUSY', 'ENDED', 'FAILED'))
);

CREATE INDEX idx_call_session_caller_status ON call_session (caller_id, status);
CREATE INDEX idx_call_session_receiver_status ON call_session (receiver_id, status);
CREATE INDEX idx_call_session_start_time ON call_session (start_time);

CREATE TABLE IF NOT EXISTS call_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    caller_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    call_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_call_log_session_id UNIQUE (session_id),
    CONSTRAINT fk_call_log_session FOREIGN KEY (session_id) REFERENCES call_session (id),
    CONSTRAINT chk_call_log_type CHECK (call_type IN ('AUDIO')),
    CONSTRAINT chk_call_log_status
        CHECK (status IN ('INITIATED', 'RINGING', 'CONNECTED', 'REJECTED', 'MISSED', 'BUSY', 'ENDED', 'FAILED'))
);

CREATE INDEX idx_call_log_caller_id ON call_log (caller_id);
CREATE INDEX idx_call_log_receiver_id ON call_log (receiver_id);
