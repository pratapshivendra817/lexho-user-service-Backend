CREATE TABLE IF NOT EXISTS cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    partner_id BIGINT NULL,
    partner_name VARCHAR(255) NULL,
    price DOUBLE NULL,
    quantity INT NULL
);

CREATE INDEX idx_cart_user_id ON cart(user_id);
CREATE INDEX idx_cart_user_partner ON cart(user_id, partner_id);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id BIGINT NULL,
    partner_name VARCHAR(255) NULL,
    price DOUBLE NULL,
    quantity INT NULL,
    order_id BIGINT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
