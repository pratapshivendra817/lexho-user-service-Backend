package com.lexho.user_service.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.user_service.config.AdminSeedProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSeedServiceTest {

    @Test
    void runDoesNothingWhenSeedingDisabled() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminSeedProperties properties = new AdminSeedProperties();
        properties.setEnabled(false);
        AdminSeedService service = new AdminSeedService(adminRepository, passwordEncoder, properties);

        service.run();

        verify(adminRepository, never()).save(any(Admin.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void runDoesNothingWhenCredentialsAreMissing() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminSeedProperties properties = new AdminSeedProperties();
        properties.setEnabled(true);
        properties.setEmail(" ");
        properties.setPassword(null);
        AdminSeedService service = new AdminSeedService(adminRepository, passwordEncoder, properties);

        service.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void runCreatesOrUpdatesAdminWithNormalizedEmailAndEncodedPassword() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminSeedProperties properties = new AdminSeedProperties();
        properties.setEnabled(true);
        properties.setEmail(" ADMIN@LEXHO.COM ");
        properties.setPassword("secret");
        Admin existing = new Admin();
        existing.setFailedAttempts(4);
        existing.setAccountLocked(true);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(adminRepository.findByEmailIgnoreCase("admin@lexho.com")).thenReturn(Optional.of(existing));
        AdminSeedService service = new AdminSeedService(adminRepository, passwordEncoder, properties);

        service.run();

        assertThat(existing.getEmail()).isEqualTo("admin@lexho.com");
        assertThat(existing.getPasswordHash()).isEqualTo("hashed");
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getFailedAttempts()).isZero();
        assertThat(existing.isAccountLocked()).isFalse();
        verify(adminRepository).save(existing);
    }
}
