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
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                environment);

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
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                new MockEnvironment());

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173", "http://localhost:4200");
    }

    @Test
    void environmentFlagsDefaultToFalseAndCanBeEnabled() {
        SecurityConfig defaults = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                new MockEnvironment());
        SecurityConfig exposed = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                new MockEnvironment()
                .withProperty("app.security.exposure.expose-swagger", "true")
                .withProperty("app.security.exposure.allow-public-customer-read", "true")
                .withProperty("app.security.exposure.allow-public-partner-onboarding", "true"));

        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isExposeSwagger")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowPublicCustomerRead")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(defaults, "isAllowPublicProviderOnboarding")).isFalse();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(exposed, "isExposeSwagger")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(exposed, "isAllowPublicCustomerRead")).isTrue();
        assertThat((boolean) ReflectionTestUtils.invokeMethod(exposed, "isAllowPublicProviderOnboarding")).isTrue();
    }

    @Test
    void publicCustomerBootstrapPathsStayEnabledThroughExposureToggle() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.exposure.allow-public-customer-read", "true");
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(InternalApiTokenFilter.class),
                environment);

        assertThat((boolean) ReflectionTestUtils.invokeMethod(securityConfig, "isAllowPublicCustomerRead")).isTrue();
    }
}
