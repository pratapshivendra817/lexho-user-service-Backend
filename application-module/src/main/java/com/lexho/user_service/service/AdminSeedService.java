package com.lexho.user_service.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.user_service.config.AdminSeedProperties;
import com.lexho.user_service.util.StringSanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminSeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedService.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminSeedProperties;

    public AdminSeedService(AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            AdminSeedProperties adminSeedProperties) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSeedProperties = adminSeedProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!adminSeedProperties.isEnabled()) {
            return;
        }

        String email = normalize(adminSeedProperties.getEmail());
        String rawPassword = adminSeedProperties.getPassword();
        if (email == null || rawPassword == null || rawPassword.trim().isEmpty()) {
            log.warn("Admin seed is enabled but admin.seed.email/password is missing");
            return;
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Admin admin = adminRepository.findByEmailIgnoreCase(email).orElseGet(Admin::new);
        admin.setEmail(email);
        admin.setPasswordHash(encodedPassword);
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.resetFailedAttempts();

        adminRepository.save(admin);
        log.info("Admin seed upserted for email: {} using encoder={}", email, passwordEncoder.getClass().getSimpleName());
    }

    private String normalize(String value) {
        String normalized = StringSanitizers.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
