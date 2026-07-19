package com.lexho.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void csrfExemptionIsLimitedToStatelessApiNamespaces() {
        assertThat(Arrays.asList(SecurityConfig.csrfExemptPaths()))
                .containsExactly("/api/**", "/internal/**", "/otp/**", "/auth/**");
    }

    @Test
    void parsesConfiguredOriginsWithoutRegexAndRemovesBlankOrDuplicateValues() {
        assertThat(SecurityConfig.parseAllowedOrigins(
                " http://localhost:5173,https://admin.lexho.com,,http://localhost:5173 "))
                .containsExactly("http://localhost:5173", "https://admin.lexho.com");
        assertThat(SecurityConfig.parseAllowedOrigins(null)).isEmpty();
        assertThat(SecurityConfig.parseAllowedOrigins("   ")).isEmpty();
    }

    @Test
    void corsConfigurationUsesConfiguredOriginsAndDisablesCredentialCookies() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "app.security.cors.allowed-origins",
                        "https://admin.lexho.com, https://customer.lexho.com");
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtFilter.class), environment);

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://admin.lexho.com", "https://customer.lexho.com");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyElementsOf(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void corsConfigurationFallsBackToDefaultOriginsWhenPropertyMissing() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtFilter.class), new MockEnvironment());

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173", "http://localhost:4200");
    }

    @Test
    void environmentFlagsDefaultToTrueAndCanBeDisabled() {
        SecurityConfig defaults = new SecurityConfig(mock(JwtFilter.class), new MockEnvironment());
        SecurityConfig lockedDown = new SecurityConfig(mock(JwtFilter.class), new MockEnvironment()
                .withProperty("app.security.exposure.expose-swagger", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("app.security.exposure.allow-public-customer-read", "false")
                .withProperty("app.security.exposure.allow-public-partner-onboarding", "false")
                .withProperty("app.security.exposure.allow-public-admin-profile-content", "false"));

        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isExposeSwagger")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowInternalOpen")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowPublicCustomerRead")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowPublicProviderOnboarding")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowPublicAdminProfileContent")).isTrue();

        assertThat((boolean) ReflectionTestUtils.invokeMethod(lockedDown, "isExposeSwagger")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(lockedDown, "isAllowInternalOpen")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(lockedDown, "isAllowPublicCustomerRead")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(lockedDown, "isAllowPublicProviderOnboarding")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(lockedDown, "isAllowPublicAdminProfileContent")).isFalse();
    }
}
