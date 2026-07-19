CREATE TABLE IF NOT EXISTS pincode_adjacency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_pincode VARCHAR(20) NOT NULL,
    adjacent_pincode VARCHAR(20) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pincode_adjacency_pair (source_pincode, adjacent_pincode),
    INDEX idx_pincode_adjacency_source_active (source_pincode, active, priority),
    INDEX idx_pincode_adjacency_adjacent (adjacent_pincode)
);

CREATE TABLE IF NOT EXISTS partner_shifts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    shift_start DATETIME NULL,
    shift_end DATETIME NULL,
    current_pincode VARCHAR(20) NULL,
    active_jobs_count INT NOT NULL DEFAULT 0,
    max_active_jobs INT NOT NULL DEFAULT 3,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_partner_shift_partner (partner_id),
    INDEX idx_partner_shifts_pincode_online (current_pincode, is_online),
    INDEX idx_partner_shifts_online_jobs (is_online, active_jobs_count)
);

CREATE TABLE IF NOT EXISTS current_partner_locations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    pincode VARCHAR(20) NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'GPS',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_current_partner_location_partner (partner_id),
    INDEX idx_current_partner_location_pincode (pincode),
    INDEX idx_current_partner_location_updated (updated_at),
    INDEX idx_partner_updated_at (partner_id, updated_at)
);

CREATE TABLE IF NOT EXISTS partner_location_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    pincode VARCHAR(20) NULL,
    captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_partner_location_history_partner_time (partner_id, captured_at)
);

CREATE TABLE IF NOT EXISTS booking_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_payload JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_booking_events_booking_time (booking_id, created_at),
    INDEX idx_booking_events_type_time (event_type, created_at)
);

SET @has_psr_target_pincode := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_service_requests'
      AND column_name = 'target_pincode'
);
SET @sql_add_psr_target_pincode := IF(
    @has_psr_target_pincode = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN target_pincode VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt_add_psr_target_pincode FROM @sql_add_psr_target_pincode;
EXECUTE stmt_add_psr_target_pincode;
DEALLOCATE PREPARE stmt_add_psr_target_pincode;

SET @has_psr_dispatch_attempt := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_service_requests'
      AND column_name = 'dispatch_attempt'
);
SET @sql_add_psr_dispatch_attempt := IF(
    @has_psr_dispatch_attempt = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN dispatch_attempt INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_add_psr_dispatch_attempt FROM @sql_add_psr_dispatch_attempt;
EXECUTE stmt_add_psr_dispatch_attempt;
DEALLOCATE PREPARE stmt_add_psr_dispatch_attempt;

SET @has_psr_distance_km := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'partner_service_requests'
      AND column_name = 'distance_km'
);
SET @sql_add_psr_distance_km := IF(
    @has_psr_distance_km = 0,
    'ALTER TABLE partner_service_requests ADD COLUMN distance_km DECIMAL(10, 3) NULL',
    'SELECT 1'
);
PREPARE stmt_add_psr_distance_km FROM @sql_add_psr_distance_km;
EXECUTE stmt_add_psr_distance_km;
DEALLOCATE PREPARE stmt_add_psr_distance_km;

CREATE INDEX idx_psr_status_partner ON partner_service_requests(status, partner_id);
CREATE INDEX idx_psr_pincode_status ON partner_service_requests(target_pincode, status);
