package com.lexho.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void csrfExemptionIncludesStatelessApiAndCallEndpoints() {
        assertThat(Arrays.asList(SecurityConfig.csrfExemptPaths()))
                .containsExactly("/api/**", "/calls/**");
    }

    @Test
    void csrfExemptionAppliesOnlyToBearerAuthenticatedStatelessRequestsWithoutCookies() {
        RequestMatcher matcher = SecurityConfig.csrfExemptRequestMatcher();

        assertThat(matcher.matches(request("POST", "/calls/start", "Bearer jwt-token", null))).isTrue();
        assertThat(matcher.matches(request("PATCH", "/api/v1/admin/serviceable-areas/9/status", "Bearer jwt-token", null))).isTrue();
        assertThat(matcher.matches(request("POST", "/calls/start", null, null))).isFalse();
        assertThat(matcher.matches(request("POST", "/calls/start", "Basic credentials", null))).isFalse();
        assertThat(matcher.matches(request("POST", "/calls/start", "Bearer jwt-token", "JSESSIONID=abc"))).isFalse();
        assertThat(matcher.matches(request("PATCH", "/api/v1/admin/serviceable-areas/9/status", "Bearer jwt-token", "JSESSIONID=abc"))).isFalse();
        assertThat(matcher.matches(request("POST", "/users/profile", "Bearer jwt-token", null))).isFalse();
        assertThat(matcher.matches(requestWithRequestUriOnly("POST", "/calls/end", "Bearer jwt-token"))).isTrue();
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
        assertThat(configuration.getAllowedMethods()).containsExactlyElementsOf(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        assertThat(configuration.getAllowedHeaders()).containsExactlyElementsOf(List.of("*"));
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    private static MockHttpServletRequest request(String method,
                                                  String path,
                                                  String authorizationHeader,
                                                  String cookieHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        if (cookieHeader != null) {
            request.addHeader("Cookie", cookieHeader);
        }
        return request;
    }

    private static MockHttpServletRequest requestWithRequestUriOnly(String method, String path, String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath("");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }
}
