CREATE TABLE IF NOT EXISTS chat_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    last_message_preview VARCHAR(500) NULL,
    last_message_at DATETIME(6) NULL,
    deleted BIT NOT NULL DEFAULT b'0',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_chat_rooms_order_id UNIQUE (order_id),
    CONSTRAINT fk_chat_rooms_order_id FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_chat_rooms_customer_id ON chat_rooms (customer_id);
CREATE INDEX idx_chat_rooms_partner_id ON chat_rooms (partner_id);
CREATE INDEX idx_chat_rooms_last_message_at ON chat_rooms (last_message_at);

CREATE TABLE IF NOT EXISTS chat_participants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    participant_type VARCHAR(20) NOT NULL,
    participant_id BIGINT NOT NULL,
    last_read_message_id BIGINT NULL,
    last_read_at DATETIME(6) NULL,
    unread_count BIGINT NOT NULL DEFAULT 0,
    deleted BIT NOT NULL DEFAULT b'0',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_chat_participants_room_actor UNIQUE (room_id, participant_type, participant_id),
    CONSTRAINT fk_chat_participants_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms (id)
);

CREATE INDEX idx_chat_participants_room_id ON chat_participants (room_id);
CREATE INDEX idx_chat_participants_actor ON chat_participants (participant_type, participant_id);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    delivered_at DATETIME(6) NULL,
    read_at DATETIME(6) NULL,
    deleted BIT NOT NULL DEFAULT b'0',
    deleted_at DATETIME(6) NULL,
    deleted_by_type VARCHAR(20) NULL,
    deleted_by_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_messages_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms (id)
);

CREATE INDEX idx_chat_messages_room_created ON chat_messages (room_id, created_at);
CREATE INDEX idx_chat_messages_sender ON chat_messages (sender_type, sender_id);
CREATE INDEX idx_chat_messages_delivery_status ON chat_messages (delivery_status);
