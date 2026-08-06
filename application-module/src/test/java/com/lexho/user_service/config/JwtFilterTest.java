package com.lexho.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private AdminRepository adminRepository;
    @Mock private FilterChain chain;

    private MockEnvironment environment;
    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment()
                .withProperty("app.security.exposure.expose-swagger", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("app.security.exposure.allow-public-customer-read", "false")
                .withProperty("app.security.exposure.allow-public-partner-onboarding", "false")
                .withProperty("app.security.exposure.allow-public-admin-profile-content", "false");
        filter = new JwtFilter(jwtUtil, new ObjectMapper(), adminRepository, environment);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/", "/error", "/favicon.ico", "/ping", "/health", "/health/readiness",
            "/api/health", "/api/health/ping", "/actuator/health", "/api/payments/webhook",
            "/otp/send", "/auth/login", "/api/otp/send", "/api/auth/login", "/uploads/a.png",
            "/images/a.png", "/api/admin/login", "/internal/auth/user", "/internal/bookings/1",
            "/api/admin/profile-content", "/api/v1/admin/profile-content",
            "/ws-notifications"
    })
    void shouldNotFilterConfiguredPublicPaths(String path) {
        MockHttpServletRequest request = request("GET", path);

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldSkipOptionsButProtectPrivateAndDisabledExposurePaths() {
        assertThat(filter.shouldNotFilter(request("OPTIONS", "/api/private"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/private"))).isFalse();
        environment.setProperty("app.security.exposure.expose-swagger", "true");
        environment.setProperty("app.security.exposure.allow-public-customer-read", "true");
        environment.setProperty("app.security.exposure.allow-public-partner-onboarding", "true");
        assertThat(filter.shouldNotFilter(request("GET", "/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/internal/auth/user"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/customer/services"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/admin/profile-content"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/v1/admin/profile-content"))).isTrue();
        assertThat(filter.shouldNotFilter(request("PUT", "/api/admin/profile-content"))).isFalse();
        assertThat(filter.shouldNotFilter(request("GET", "/api/partner/onboarding"))).isTrue();
    }

    @Test
    void publicOtpRoutePassesThroughFullFilterLifecycleWithoutAuthorization() throws Exception {
        MockHttpServletRequest request = request("POST", "/otp/send");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(401);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, adminRepository);
    }

    @Test
    void internalTokenDoesNotBypassJwtAuthenticationForApiNamespaces() throws Exception {
        environment.setProperty("INTERNAL_API_TOKEN", "secret");
        MockHttpServletRequest admin = request("GET", "/api/admin/dashboard");
        admin.addHeader("X-Internal-Token", "secret");
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();

        filter.doFilterInternal(admin, adminResponse, chain);

        assertThat(adminResponse.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void missingAndBlankBearerTokensReturnUnauthorizedContracts() throws Exception {
        MockHttpServletRequest customer = request("GET", "/api/private");
        MockHttpServletResponse customerResponse = new MockHttpServletResponse();
        filter.doFilterInternal(customer, customerResponse, chain);

        assertThat(customerResponse.getStatus()).isEqualTo(401);
        assertThat(customerResponse.getContentAsString()).contains("Authorization token is required", "\"success\":false");

        environment.setProperty("app.security.exposure.allow-internal-open", "false");
        MockHttpServletRequest admin = request("GET", "/api/admin/users");
        admin.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        filter.doFilterInternal(admin, adminResponse, chain);

        assertThat(adminResponse.getStatus()).isEqualTo(401);
        assertThat(adminResponse.getContentAsString()).contains("\"message\":\"Authorization token is required\"");
        verifyNoInteractions(chain);
    }

    @Test
    void validCustomerTokenPopulatesAuthenticationAndRequestAttributes() throws Exception {
        Claims claims = claims(7L, "9999999999", "CUSTOMER", "user@example.com");
        when(jwtUtil.validateToken("valid")).thenReturn(claims);
        MockHttpServletRequest request = request("GET", "/api/customer/cart");
        request.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute("userId")).isEqualTo(7L);
        assertThat(request.getAttribute("role")).isEqualTo("CUSTOMER");
        assertThat(request.getAttribute("email")).isEqualTo("user@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails()).isEqualTo(7L);
        verify(chain).doFilter(request, response);
    }

    @Test
    void validActiveAdminTokenPassesWhileInvalidAdminAndMalformedTokensFail() throws Exception {
        environment.setProperty("app.security.exposure.allow-internal-open", "false");
        Admin admin = mock(Admin.class);
        when(admin.isActive()).thenReturn(true);
        when(admin.getId()).thenReturn(9L);
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        Claims validAdminClaims = claims(9L, "admin", "ADMIN", "admin@example.com");
        when(jwtUtil.validateToken("admin")).thenReturn(validAdminClaims);
        MockHttpServletRequest valid = request("GET", "/api/admin/users");
        valid.addHeader("Authorization", "Bearer admin");
        MockHttpServletResponse validResponse = new MockHttpServletResponse();

        filter.doFilterInternal(valid, validResponse, chain);
        verify(chain).doFilter(valid, validResponse);

        SecurityContextHolder.clearContext();
        Claims invalidAdminClaims = claims(10L, "admin", "ADMIN", "");
        when(jwtUtil.validateToken("bad-admin")).thenReturn(invalidAdminClaims);
        MockHttpServletRequest invalid = request("GET", "/api/admin/users");
        invalid.addHeader("Authorization", "Bearer bad-admin");
        MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        filter.doFilterInternal(invalid, invalidResponse, chain);
        assertThat(invalidResponse.getStatus()).isEqualTo(401);

        when(jwtUtil.validateToken("broken")).thenThrow(new IllegalArgumentException("expired"));
        MockHttpServletRequest broken = request("GET", "/api/private");
        broken.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse brokenResponse = new MockHttpServletResponse();
        filter.doFilterInternal(broken, brokenResponse, chain);
        assertThat(brokenResponse.getContentAsString()).contains("Invalid or expired token");
    }

    private Claims claims(Long userId, String subject, String role, String email) {
        Claims claims = mock(Claims.class);
        when(claims.get("userId")).thenReturn(userId);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.get("email", String.class)).thenReturn(email);
        return claims;
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
