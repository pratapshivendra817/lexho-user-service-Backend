package com.lexho.user_service.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigCoverageAdditionalTest {

    @Test
    void adminAuthPropertiesExposeDefaultsAndAllowMutation() {
        AdminAuthProperties properties = new AdminAuthProperties();

        assertThat(properties.getOtp().getKeyPrefix()).isEqualTo("ADMIN_OTP_");
        assertThat(properties.getOtp().getExpirySeconds()).isEqualTo(300);
        assertThat(properties.getOtp().getMaxAttempts()).isEqualTo(3);
        assertThat(properties.getOtp().getDeliveryFrom()).isEqualTo("no-reply@lexho.com");
        assertThat(properties.getPassword().getMaxFailedAttempts()).isEqualTo(5);
        assertThat(properties.getPassword().getLockDurationMinutes()).isEqualTo(30);
        assertThat(properties.getRateLimit().getMaxRequests()).isEqualTo(5);
        assertThat(properties.getRateLimit().getWindowSeconds()).isEqualTo(900);
        assertThat(properties.getRateLimit().getKeyPrefix()).isEqualTo("ADMIN_LOGIN_RL_");
        assertThat(properties.getInfrastructure().isAllowInMemoryOtpFallback()).isFalse();
        assertThat(properties.getInfrastructure().isAllowInMemoryRateLimitFallback()).isFalse();
        assertThat(properties.getInfrastructure().isAllowConsoleOtpDeliveryFallback()).isFalse();

        properties.getOtp().setKeyPrefix("OTP_");
        properties.getOtp().setExpirySeconds(120);
        properties.getOtp().setMaxAttempts(5);
        properties.getOtp().setDeliveryFrom("ops@lexho.com");
        properties.getPassword().setMaxFailedAttempts(8);
        properties.getPassword().setLockDurationMinutes(45);
        properties.getRateLimit().setMaxRequests(7);
        properties.getRateLimit().setWindowSeconds(60);
        properties.getRateLimit().setKeyPrefix("RL_");
        properties.getInfrastructure().setAllowInMemoryOtpFallback(true);
        properties.getInfrastructure().setAllowInMemoryRateLimitFallback(true);
        properties.getInfrastructure().setAllowConsoleOtpDeliveryFallback(true);

        assertThat(properties.getOtp().getKeyPrefix()).isEqualTo("OTP_");
        assertThat(properties.getOtp().getExpirySeconds()).isEqualTo(120);
        assertThat(properties.getOtp().getMaxAttempts()).isEqualTo(5);
        assertThat(properties.getOtp().getDeliveryFrom()).isEqualTo("ops@lexho.com");
        assertThat(properties.getPassword().getMaxFailedAttempts()).isEqualTo(8);
        assertThat(properties.getPassword().getLockDurationMinutes()).isEqualTo(45);
        assertThat(properties.getRateLimit().getMaxRequests()).isEqualTo(7);
        assertThat(properties.getRateLimit().getWindowSeconds()).isEqualTo(60);
        assertThat(properties.getRateLimit().getKeyPrefix()).isEqualTo("RL_");
        assertThat(properties.getInfrastructure().isAllowInMemoryOtpFallback()).isTrue();
        assertThat(properties.getInfrastructure().isAllowInMemoryRateLimitFallback()).isTrue();
        assertThat(properties.getInfrastructure().isAllowConsoleOtpDeliveryFallback()).isTrue();
    }

    @Test
    void appAndSeedPropertiesExposeDefaultsAndAllowMutation() {
        AppProperties appProperties = new AppProperties();
        AdminSeedProperties seedProperties = new AdminSeedProperties();
        CorsProperties corsProperties = new CorsProperties();

        assertThat(appProperties.getUser().getDefaultCity()).isEqualTo("Unknown");
        assertThat(appProperties.getHome().getPrimaryPromoLabel()).isEqualTo("Limited Offer");
        assertThat(appProperties.getHome().getPrimaryCtaLabel()).isEqualTo("Book Now");
        assertThat(appProperties.getHome().getSecondaryCtaLabel()).isEqualTo("View Offers");
        assertThat(appProperties.getHome().getRecommendationTagline()).isEqualTo("Next available slot soon");
        assertThat(appProperties.getHome().getFallbackGreeting()).isEqualTo("Hi");
        assertThat(seedProperties.isEnabled()).isFalse();
        assertThat(corsProperties.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173");

        appProperties.getUser().setDefaultCity("Pune");
        appProperties.getHome().setPrimaryPromoLabel("Flash");
        appProperties.getHome().setPrimaryCtaLabel("Start");
        appProperties.getHome().setSecondaryCtaLabel("Later");
        appProperties.getHome().setRecommendationTagline("Soon");
        appProperties.getHome().setFallbackGreeting("Hello");
        seedProperties.setEnabled(true);
        seedProperties.setEmail("admin@lexho.com");
        seedProperties.setPassword("secret");
        corsProperties.setAllowedOrigins(List.of("https://app.lexho.com"));

        assertThat(appProperties.getUser().getDefaultCity()).isEqualTo("Pune");
        assertThat(appProperties.getHome().getPrimaryPromoLabel()).isEqualTo("Flash");
        assertThat(appProperties.getHome().getPrimaryCtaLabel()).isEqualTo("Start");
        assertThat(appProperties.getHome().getSecondaryCtaLabel()).isEqualTo("Later");
        assertThat(appProperties.getHome().getRecommendationTagline()).isEqualTo("Soon");
        assertThat(appProperties.getHome().getFallbackGreeting()).isEqualTo("Hello");
        assertThat(seedProperties.isEnabled()).isTrue();
        assertThat(seedProperties.getEmail()).isEqualTo("admin@lexho.com");
        assertThat(seedProperties.getPassword()).isEqualTo("secret");
        assertThat(corsProperties.getAllowedOrigins()).containsExactly("https://app.lexho.com");
    }

    @Test
    void apiVersionAndCorrelationFiltersSetExpectedHeaders() throws Exception {
        ApiVersionHeaderFilter apiVersionHeaderFilter = new ApiVersionHeaderFilter();
        CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();

        MockHttpServletRequest v1Request = request("GET", "/api/v1/customer/services");
        MockHttpServletResponse v1Response = new MockHttpServletResponse();
        apiVersionHeaderFilter.doFilterInternal(v1Request, v1Response, passThroughChain());
        assertThat(v1Response.getHeader("X-API-Version")).isEqualTo("v1");
        assertThat(v1Response.getHeader("X-API-Legacy-Route")).isNull();

        MockHttpServletRequest legacyRequest = request("GET", "/api/customer/services");
        MockHttpServletResponse legacyResponse = new MockHttpServletResponse();
        apiVersionHeaderFilter.doFilterInternal(legacyRequest, legacyResponse, passThroughChain());
        assertThat(legacyResponse.getHeader("X-API-Legacy-Route")).isEqualTo("true");

        MockHttpServletRequest publicRequest = request("GET", "/public");
        MockHttpServletResponse publicResponse = new MockHttpServletResponse();
        apiVersionHeaderFilter.doFilterInternal(publicRequest, publicResponse, passThroughChain());
        assertThat(publicResponse.getHeader("X-API-Version")).isNull();
        assertThat(publicResponse.getHeader("X-API-Legacy-Route")).isNull();

        MockHttpServletRequest providedIds = request("GET", "/api/customer/services");
        providedIds.addHeader(CorrelationIdFilter.CORRELATION_HEADER, "corr-1");
        providedIds.addHeader(CorrelationIdFilter.REQUEST_HEADER, "req-1");
        MockHttpServletResponse providedResponse = new MockHttpServletResponse();
        correlationIdFilter.doFilterInternal(providedIds, providedResponse, passThroughChain());
        assertThat(providedResponse.getHeader(CorrelationIdFilter.CORRELATION_HEADER)).isEqualTo("corr-1");
        assertThat(providedResponse.getHeader(CorrelationIdFilter.REQUEST_HEADER)).isEqualTo("req-1");

        MockHttpServletRequest generatedIds = request("GET", "/api/customer/services");
        MockHttpServletResponse generatedResponse = new MockHttpServletResponse();
        correlationIdFilter.doFilterInternal(generatedIds, generatedResponse, passThroughChain());
        assertThat(generatedResponse.getHeader(CorrelationIdFilter.CORRELATION_HEADER)).isNotBlank();
        assertThat(generatedResponse.getHeader(CorrelationIdFilter.REQUEST_HEADER)).isNotBlank();
    }

    @Test
    void cryptoRedisAndStaticResourceConfigExposeExpectedBeans() {
        PasswordEncoder encoder = new CryptoConfig().passwordEncoder();
        assertThat(encoder.encode("password")).isNotBlank();
        assertThat(encoder.matches("password", encoder.encode("password"))).isTrue();

        RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);
        RedisTemplate<String, String> template = new RedisConfig().redisTemplate(connectionFactory);
        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(StringRedisSerializer.class);

        StaticResourceConfig staticResourceConfig = new StaticResourceConfig("uploads");
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new StaticApplicationContext(), new MockServletContext());
        staticResourceConfig.addResourceHandlers(registry);
        assertThat(registry.hasMappingForPattern("/images/**")).isTrue();
        assertThat(registry.hasMappingForPattern("/uploads/**")).isTrue();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static FilterChain passThroughChain() {
        return (request, response) -> { };
    }
}
