package com.lexho.user_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
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
        List<String> missing = new ArrayList<>();

        require("DB_PASSWORD", missing);
        require("JWT_SECRET", missing);

        boolean adminSeedEnabled = Boolean.parseBoolean(environment.getProperty("ADMIN_SEED_ENABLED", "true"));
        if (adminSeedEnabled) {
            require("ADMIN_SEED_PASSWORD", missing);
        }
        boolean allowInternalOpen = Boolean.parseBoolean(environment.getProperty("APP_SECURITY_ALLOW_INTERNAL_OPEN", "true"));
        if (!allowInternalOpen) {
            require("INTERNAL_API_TOKEN", missing);
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required environment variable(s): " + String.join(", ", missing));
        }
    }

    private void require(String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            missing.add(key);
        }
    }
}
