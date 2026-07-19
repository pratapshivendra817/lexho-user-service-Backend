CREATE TABLE IF NOT EXISTS addons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    addon_name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    price DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    quantity_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    min_quantity INT NOT NULL DEFAULT 1,
    max_quantity INT NOT NULL DEFAULT 1,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_addons_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_addons_min_quantity CHECK (min_quantity >= 1),
    CONSTRAINT chk_addons_max_quantity CHECK (max_quantity >= min_quantity),
    INDEX idx_addons_active (active),
    INDEX idx_addons_name (addon_name)
);

CREATE TABLE IF NOT EXISTS category_addons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_category_addons_category FOREIGN KEY (category_id) REFERENCES admin_categories(id),
    CONSTRAINT fk_category_addons_addon FOREIGN KEY (addon_id) REFERENCES addons(id),
    UNIQUE KEY uk_category_addons_category_addon (category_id, addon_id),
    INDEX idx_category_addons_category (category_id),
    INDEX idx_category_addons_addon (addon_id)
);

CREATE TABLE IF NOT EXISTS service_addons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_service_addons_service FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_service_addons_addon FOREIGN KEY (addon_id) REFERENCES addons(id),
    UNIQUE KEY uk_service_addons_service_addon (service_id, addon_id),
    INDEX idx_service_addons_service (service_id),
    INDEX idx_service_addons_addon (addon_id)
);

CREATE TABLE IF NOT EXISTS cart_item_addons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_item_addons_cart_item FOREIGN KEY (cart_item_id) REFERENCES cart_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_addons_addon FOREIGN KEY (addon_id) REFERENCES addons(id),
    CONSTRAINT chk_cart_item_addons_quantity CHECK (quantity >= 1),
    UNIQUE KEY uk_cart_item_addons_cart_addon (cart_item_id, addon_id),
    INDEX idx_cart_item_addons_user (user_id),
    INDEX idx_cart_item_addons_service (service_id)
);

CREATE TABLE IF NOT EXISTS order_addons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    addon_name_snapshot VARCHAR(150) NOT NULL,
    addon_price_snapshot DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_addons_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_addons_quantity CHECK (quantity >= 1),
    INDEX idx_order_addons_order (order_id),
    INDEX idx_order_addons_service (service_id),
    INDEX idx_order_addons_addon (addon_id)
);
