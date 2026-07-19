ALTER TABLE push_device_tokens
    ADD COLUMN platform VARCHAR(30) NULL AFTER token,
    ADD COLUMN app_type VARCHAR(30) NULL AFTER platform;
