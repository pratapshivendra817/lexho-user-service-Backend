/*
 * Non-destructive duplicate audit.
 *
 * partners.id is the canonical identity. Existing rows are never deleted or
 * remapped automatically because booking, order, payout and payment tables
 * reference the numeric partner id.
 */
CREATE TABLE IF NOT EXISTS partner_identity_reconciliation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    issue_type VARCHAR(40) NOT NULL,
    canonical_partner_id BIGINT NULL,
    legacy_partner_id BIGINT NULL,
    identity_key VARCHAR(255) NULL,
    related_partner_ids VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEEDS_REVIEW',
    details VARCHAR(1000) NULL,
    detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_partner_reconciliation_status (status),
    KEY idx_partner_reconciliation_type (issue_type),
    KEY idx_partner_reconciliation_canonical (canonical_partner_id),
    CONSTRAINT fk_partner_reconciliation_canonical
        FOREIGN KEY (canonical_partner_id) REFERENCES partners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO partner_identity_reconciliation (
    issue_type, identity_key, related_partner_ids, details
)
SELECT
    'DUPLICATE_EMAIL',
    LOWER(TRIM(email)),
    GROUP_CONCAT(id ORDER BY id),
    'Multiple active canonical partners share the same normalized email'
FROM partners
WHERE deleted = 0
  AND email IS NOT NULL
  AND TRIM(email) <> ''
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;

INSERT INTO partner_identity_reconciliation (
    issue_type, identity_key, related_partner_ids, details
)
SELECT
    'DUPLICATE_PHONE',
    TRIM(phone_number),
    GROUP_CONCAT(id ORDER BY id),
    'Multiple active canonical partners share the same normalized phone number'
FROM partners
WHERE deleted = 0
  AND phone_number IS NOT NULL
  AND TRIM(phone_number) <> ''
GROUP BY TRIM(phone_number)
HAVING COUNT(*) > 1;

INSERT INTO partner_identity_reconciliation (
    issue_type, legacy_partner_id, identity_key, related_partner_ids, details
)
SELECT
    'ORPHAN_LEGACY_ROW',
    p.id,
    CONCAT('legacy:', p.id),
    CAST(p.id AS CHAR),
    'Legacy marketplace row has no canonical partners row'
FROM partner p
LEFT JOIN partners cp ON cp.id = p.id
WHERE cp.id IS NULL;

INSERT INTO partner_identity_reconciliation (
    issue_type, canonical_partner_id, identity_key, related_partner_ids, details
)
SELECT
    'MISSING_LEGACY_PROJECTION',
    cp.id,
    CONCAT('canonical:', cp.id),
    CAST(cp.id AS CHAR),
    'Active canonical partner has no legacy marketplace projection'
FROM partners cp
LEFT JOIN partner p ON p.id = cp.id
WHERE cp.deleted = 0
  AND p.id IS NULL;
