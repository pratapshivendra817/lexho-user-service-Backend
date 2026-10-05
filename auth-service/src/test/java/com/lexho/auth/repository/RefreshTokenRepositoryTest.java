package com.lexho.auth.repository;

import com.lexho.auth.entity.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository repository;

    @Test
    void findByTokenShouldReturnPresentAndAbsentCases() {
        RefreshToken token = new RefreshToken();
        token.setUserId(10L);
        token.setToken("refresh-token");
        token.setRole("ADMIN");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setRevoked(false);

        RefreshToken saved = repository.saveAndFlush(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(repository.findByToken("refresh-token")).contains(saved);
        assertThat(repository.findByToken("missing")).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = RefreshToken.class)
    @EnableJpaRepositories(basePackageClasses = RefreshTokenRepository.class)
    static class TestConfiguration {
    }
}
