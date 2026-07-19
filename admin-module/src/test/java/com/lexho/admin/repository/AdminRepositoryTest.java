package com.lexho.admin.repository;

import com.lexho.admin.entity.Admin;
import com.lexho.common.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdminRepositoryTest {

    @Autowired
    private AdminRepository adminRepository;

    @Test
    void saveFindUpdateAndDeleteAdmin() {
        Admin admin = new Admin();
        admin.setEmail("  Admin@Lexho.com ");
        admin.setPasswordHash("hash-1");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        Admin saved = adminRepository.saveAndFlush(admin);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("admin@lexho.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Admin> fetched = adminRepository.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getPasswordHash()).isEqualTo("hash-1");

        fetched.get().setPassword("hash-2");
        fetched.get().setEmail("  updated@lexho.com ");
        Admin updated = adminRepository.saveAndFlush(fetched.get());

        assertThat(updated.getPassword()).isEqualTo("hash-2");
        assertThat(updated.getEmail()).isEqualTo("updated@lexho.com");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());

        adminRepository.delete(updated);
        adminRepository.flush();

        assertThat(adminRepository.findById(updated.getId())).isEmpty();
    }

    @Test
    void findByEmailIgnoreCaseMatchesStoredAdminRegardlessOfCase() {
        Admin admin = new Admin();
        admin.setEmail("Support@Lexho.com");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        adminRepository.saveAndFlush(admin);

        Optional<Admin> found = adminRepository.findByEmailIgnoreCase("support@lexho.COM");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("support@lexho.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void findByEmailIgnoreCaseReturnsEmptyWhenNoAdminExists() {
        assertThat(adminRepository.findByEmailIgnoreCase("missing@lexho.com")).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = AdminRepository.class)
    @EntityScan(basePackageClasses = Admin.class)
    static class TestConfiguration {
    }
}
