SET @addr_pincode := (
    SELECT pincode
    FROM lexho_addresses
    WHERE id = 3
    LIMIT 1
);

UPDATE partner
SET available = TRUE,
    verification_status = 'VERIFIED'
WHERE id = 4;

UPDATE partner_shifts
SET is_online = TRUE,
    current_pincode = @addr_pincode
WHERE partner_id = 4;
