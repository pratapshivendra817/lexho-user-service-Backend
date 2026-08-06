package com.lexho.user_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RequiredEnvStartupGuard implements ApplicationRunner {

    private final Environment environment;

    public RequiredEnvStartupGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }
        List<String> missing = new ArrayList<>();

        requireProperty("spring.datasource.url", missing);
        requireProperty("spring.datasource.username", missing);
        requireProperty("spring.datasource.password", missing);
        rejectInsecureJdbcUrl();
        requireProperty("security.jwt.secret", missing);
        rejectDefaultJwtSecret();

        boolean adminSeedEnabled = getBoolean("admin.seed.enabled", "ADMIN_SEED_ENABLED", false);
        if (adminSeedEnabled) {
            requireProperty("admin.seed.email", missing);
            requireProperty("admin.seed.password", missing);
            rejectDefaultAdminSeedPassword();
        }
        boolean allowInternalOpen = getBoolean(
                "app.security.exposure.allow-internal-open",
                "APP_SECURITY_ALLOW_INTERNAL_OPEN",
                false);
        if (!allowInternalOpen) {
            require("INTERNAL_API_TOKEN", missing);
            rejectWeakInternalApiToken();
        }
        rejectUnsafeProductionExposure();

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required environment variable(s): " + String.join(", ", missing));
        }
    }

    private boolean isProductionProfile() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    private void require(String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            missing.add(key);
        }
    }

    private void requireProperty(String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value == null || value.trim().isEmpty() || value.startsWith("${")) {
            missing.add(key);
        }
    }

    private void rejectDefaultJwtSecret() {
        String jwtSecret = environment.getProperty("security.jwt.secret", "");
        if (jwtSecret.length() < 32
                || jwtSecret.contains("dev-only-local-jwt-secret")
                || jwtSecret.contains("change-this")
                || jwtSecret.contains("replace-with")
                || "12345678901234567890123456789012".equals(jwtSecret)) {
            throw new IllegalStateException("security.jwt.secret must be a production-grade secret with at least 32 characters");
        }
    }

    private void rejectInsecureJdbcUrl() {
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        String normalized = jdbcUrl.toLowerCase();
        if (normalized.contains("usessl=false")
                || normalized.contains("sslmode=disabled")
                || normalized.contains("sslmode=preferred")
                || normalized.contains("allowpublickeyretrieval=true")) {
            throw new IllegalStateException("spring.datasource.url must require TLS-safe MySQL settings in production");
        }
    }

    private void rejectDefaultAdminSeedPassword() {
        String seedPassword = environment.getProperty("admin.seed.password", "");
        if ("Admin@12345!".equals(seedPassword)
                || seedPassword.contains("replace-with")
                || seedPassword.length() < 12) {
            throw new IllegalStateException("admin.seed.password must be changed before enabling admin seed in production");
        }
    }

    private void rejectWeakInternalApiToken() {
        String internalApiToken = environment.getProperty("INTERNAL_API_TOKEN", "");
        if (internalApiToken.isBlank()) {
            return;
        }
        if ("change-me".equals(internalApiToken)
                || internalApiToken.contains("replace-with")
                || internalApiToken.length() < 24) {
            throw new IllegalStateException("INTERNAL_API_TOKEN must be changed before production startup");
        }
    }

    private void rejectUnsafeProductionExposure() {
        requireFalse("app.security.exposure.expose-swagger");
        requireFalse("app.security.exposure.allow-internal-open");
        requireFalse("app.security.exposure.allow-public-customer-read");
        requireFalse("app.security.exposure.allow-public-partner-onboarding");
        requireFalse("app.security.exposure.allow-public-admin-profile-content");
        requireFalse("app.security.exposure.allow-partner-request-dev-seed");
        requireFalse("admin.auth.infrastructure.allow-in-memory-otp-fallback");
        requireFalse("admin.auth.infrastructure.allow-in-memory-rate-limit-fallback");
        requireFalse("admin.auth.infrastructure.allow-console-otp-delivery-fallback");
        requireFalse("lexho.auth.otp.log-to-console");
    }

    private void requireFalse(String key) {
        if (getBoolean(key, null, false)) {
            throw new IllegalStateException(key + " must be false in production");
        }
    }

    private boolean getBoolean(String propertyKey, String environmentKey, boolean defaultValue) {
        String value = environment.getProperty(propertyKey);
        if (value == null && environmentKey != null) {
            value = environment.getProperty(environmentKey);
        }
        if (value == null || value.trim().isEmpty() || value.startsWith("${")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
