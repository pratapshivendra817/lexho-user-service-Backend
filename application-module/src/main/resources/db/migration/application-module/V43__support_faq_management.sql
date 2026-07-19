CREATE TABLE IF NOT EXISTS support_faq_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_key VARCHAR(100) NOT NULL,
    label VARCHAR(150) NOT NULL,
    display_order INT NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_faq_categories_key (category_key)
);

CREATE TABLE IF NOT EXISTS support_faqs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    faq_key VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    question VARCHAR(300) NOT NULL,
    answer VARCHAR(4000) NOT NULL,
    display_order INT NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_faqs_key (faq_key),
    KEY idx_support_faqs_category (category_id),
    CONSTRAINT fk_support_faqs_category FOREIGN KEY (category_id) REFERENCES support_faq_categories (id) ON DELETE CASCADE
);

INSERT INTO support_faq_categories (category_key, label, display_order, active)
SELECT 'frequently-asked', 'Frequently Asked', 1, b'1'
WHERE NOT EXISTS (SELECT 1 FROM support_faq_categories WHERE category_key = 'frequently-asked');

INSERT INTO support_faq_categories (category_key, label, display_order, active)
SELECT 'payments', 'Payments', 2, b'1'
WHERE NOT EXISTS (SELECT 1 FROM support_faq_categories WHERE category_key = 'payments');

INSERT INTO support_faqs (faq_key, category_id, question, answer, display_order, active)
SELECT 'booking-cancelled', c.id, 'Why was my booking cancelled?', 'A booking may cancel if partner assignment fails...', 1, b'1'
FROM support_faq_categories c
WHERE c.category_key = 'frequently-asked'
  AND NOT EXISTS (SELECT 1 FROM support_faqs WHERE faq_key = 'booking-cancelled');

INSERT INTO support_faqs (faq_key, category_id, question, answer, display_order, active)
SELECT 'refund-pending', c.id, 'I have not received my refund', 'Refunds usually reflect within 3-7 business days.', 1, b'1'
FROM support_faq_categories c
WHERE c.category_key = 'payments'
  AND NOT EXISTS (SELECT 1 FROM support_faqs WHERE faq_key = 'refund-pending');

