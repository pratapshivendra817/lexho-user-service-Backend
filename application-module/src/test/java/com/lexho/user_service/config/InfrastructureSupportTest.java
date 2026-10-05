package com.lexho.user_service.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InfrastructureSupportTest {

    @Test
    void requestTraceFilterSkipsNonApiPathsAndIncludesHealthAndOtpPaths() throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();

        assertThat(filter.shouldNotFilter(request("GET", "/assets/app.js"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/api/customer/services"))).isFalse();
        assertThat(filter.shouldNotFilter(request("GET", "/health"))).isFalse();
        assertThat(filter.shouldNotFilter(request("GET", "/otp/send"))).isFalse();
        assertThat(filter.shouldNotFilter(requestWithNullPath())).isTrue();
    }

    @Test
    void requestTraceFilterPassesThroughAndHandlesBookingIdsFromPathAndQuery() throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = request("GET", "/api/bookings/123/items");
        request.setParameter("bookingId", "456");
        request.setAttribute("userId", 77L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(201);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void internalApiTokenFilterSkipsAllowedPathsAndRejectsMissingOrInvalidToken() throws Exception {
        SecurityExposureProperties properties = new SecurityExposureProperties();
        properties.setAllowInternalOpen(false);
        MockEnvironment environment = new MockEnvironment().withProperty("INTERNAL_API_TOKEN", "secret");
        InternalApiTokenFilter filter = new InternalApiTokenFilter(properties, environment);

        assertThat(filter.shouldNotFilter(request("OPTIONS", "/internal/orders"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/public"))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/internal/orders"))).isFalse();

        MockHttpServletRequest request = request("GET", "/internal/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain unauthorizedChain = (req, res) -> { };
        filter.doFilterInternal(request, response, unauthorizedChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized internal API access");

        request.addHeader("X-Internal-Token", "secret");
        response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(204);
        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void startupGuardRequiresExpectedEnvironmentVariables() {
        MockEnvironment valid = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com:3306/app?sslMode=REQUIRED")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("security.jwt.secret", "abcdefghijklmnopqrstuvwxyz123456")
                .withProperty("admin.seed.enabled", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("INTERNAL_API_TOKEN", "internal-token-abcdefghijklmnopqrstuvwxyz");
        valid.setActiveProfiles("prod");

        new RequiredEnvStartupGuard(valid).run(new DefaultApplicationArguments(new String[0]));

        MockEnvironment invalid = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com:3306/app?sslMode=REQUIRED")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("security.jwt.secret", "abcdefghijklmnopqrstuvwxyz123456")
                .withProperty("admin.seed.enabled", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false");
        invalid.setActiveProfiles("prod");

        assertThatThrownBy(() -> new RequiredEnvStartupGuard(invalid).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_TOKEN");
    }

    @Test
    void startupGuardRejectsWeakProductionDefaults() {
        MockEnvironment invalid = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com:3306/app?sslMode=REQUIRED")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("security.jwt.secret", "change-this-to-a-long-random-secret-key-min-32-bytes")
                .withProperty("admin.seed.enabled", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("INTERNAL_API_TOKEN", "internal-token-abcdefghijklmnopqrstuvwxyz");
        invalid.setActiveProfiles("prod");

        assertThatThrownBy(() -> new RequiredEnvStartupGuard(invalid).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.jwt.secret");
    }

    @Test
    void startupGuardRejectsDevOnlyJwtFallbackInProduction() {
        MockEnvironment invalid = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com:3306/app?sslMode=REQUIRED")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("security.jwt.secret", "dev-only-local-jwt-secret-minimum-32-characters")
                .withProperty("admin.seed.enabled", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("INTERNAL_API_TOKEN", "internal-token-abcdefghijklmnopqrstuvwxyz");
        invalid.setActiveProfiles("prod");

        assertThatThrownBy(() -> new RequiredEnvStartupGuard(invalid).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.jwt.secret");
    }

    @Test
    void startupGuardRejectsInsecureMysqlTransportSettingsInProduction() {
        MockEnvironment invalid = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/app?useSSL=false&allowPublicKeyRetrieval=true")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("security.jwt.secret", "abcdefghijklmnopqrstuvwxyz123456")
                .withProperty("admin.seed.enabled", "false")
                .withProperty("app.security.exposure.allow-internal-open", "false")
                .withProperty("INTERNAL_API_TOKEN", "internal-token-abcdefghijklmnopqrstuvwxyz");
        invalid.setActiveProfiles("prod");

        assertThatThrownBy(() -> new RequiredEnvStartupGuard(invalid).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.url")
                .hasMessageContaining("TLS-safe");
    }

    @Test
    void securityExposurePropertiesExposeDefaultsAndMutations() {
        SecurityExposureProperties properties = new SecurityExposureProperties();

        assertThat(properties.isExposeSwagger()).isFalse();
        assertThat(properties.isAllowInternalOpen()).isFalse();
        assertThat(properties.isAllowPublicCustomerRead()).isFalse();
        assertThat(properties.isAllowPublicAdminProfileContent()).isFalse();

        properties.setExposeSwagger(true);
        properties.setAllowInternalOpen(true);
        properties.setAllowPublicCustomerRead(true);
        properties.setAllowPublicAdminProfileContent(true);

        assertThat(properties.isExposeSwagger()).isTrue();
        assertThat(properties.isAllowInternalOpen()).isTrue();
        assertThat(properties.isAllowPublicCustomerRead()).isTrue();
        assertThat(properties.isAllowPublicAdminProfileContent()).isTrue();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static MockHttpServletRequest requestWithNullPath() {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        return request;
    }
}
