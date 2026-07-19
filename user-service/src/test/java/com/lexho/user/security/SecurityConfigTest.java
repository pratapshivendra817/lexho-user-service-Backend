package com.lexho.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void csrfExemptionIsLimitedToCallEndpoints() {
        assertThat(Arrays.asList(SecurityConfig.csrfExemptPaths()))
                .containsExactly("/calls/**");
    }

    @Test
    void parsesConfiguredOriginsWithoutWildcardsAndRemovesDuplicates() {
        assertThat(SecurityConfig.parseAllowedOrigins(
                " http://localhost:5173,https://app.lexho.com,,http://localhost:5173 "))
                .containsExactly("http://localhost:5173", "https://app.lexho.com");
        assertThat(SecurityConfig.parseAllowedOrigins(null)).isEmpty();
        assertThat(SecurityConfig.parseAllowedOrigins("   ")).isEmpty();
    }

    @Test
    void corsConfigurationUsesConfiguredOriginsAndDisablesCredentialCookies() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.cors.allowed-origins",
                        "https://app.lexho.com, https://admin.lexho.com");
        SecurityConfig securityConfig = new SecurityConfig(environment);

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://app.lexho.com", "https://admin.lexho.com");
        assertThat(configuration.getAllowedMethods()).containsExactlyElementsOf(List.of("GET", "POST", "OPTIONS"));
        assertThat(configuration.getAllowedHeaders()).containsExactlyElementsOf(List.of("*"));
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
