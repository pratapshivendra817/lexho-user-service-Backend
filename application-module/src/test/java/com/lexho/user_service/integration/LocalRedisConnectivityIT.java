package com.lexho.user_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.service.AdminDashboardService;
import com.lexho.user_service.admin.service.AdminMonitoringService;
import com.lexho.user_service.admin.service.RestrictedAreaOpsService;
import com.lexho.user_service.admin.service.ScaleProfileService;
import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.repository.ServiceablePincodeRepository;
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import com.lexho.user_service.customer.location.service.impl.ServiceabilityServiceImpl;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerGeoIndexService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleState;
import com.lexho.user_service.exception.TooManyRequestsException;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.service.NavigationEtaTrackingRegistry;
import com.lexho.user_service.map.service.NavigationRouteCacheService;
import com.lexho.user_service.service.AdminLoginRateLimiterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("infra-it")
class LocalRedisConnectivityIT {

    @Test
    void shouldConnectToLocalRedisAndExerciseRedisBackedFlows() throws Exception {
        RedisSettings settings = RedisSettings.load();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(settings.host(), settings.port());
        if (settings.password() != null && !settings.password().isBlank()) {
            connectionFactory.setPassword(settings.password());
        }
        connectionFactory.setValidateConnection(true);
        connectionFactory.afterPropertiesSet();

        try {
            StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

            assertThat(redisTemplate.getConnectionFactory().getConnection().ping()).isEqualToIgnoringCase("PONG");

            String directKey = "it:redis:direct";
            redisTemplate.opsForValue().set(directKey, "ok", Duration.ofSeconds(30));
            assertThat(redisTemplate.opsForValue().get(directKey)).isEqualTo("ok");
            assertThat(redisTemplate.getExpire(directKey)).isPositive();

            verifyReverseGeocodeCache(redisTemplate);
            verifyServiceabilityCache(redisTemplate);
            verifyNavigationRouteCache(redisTemplate);
            verifyPartnerAppRedisBackedFlows(redisTemplate);
            verifyAdminAppRedisBackedFlows(redisTemplate);
        } finally {
            connectionFactory.destroy();
        }
    }

    private static void verifyReverseGeocodeCache(StringRedisTemplate redisTemplate) {
        MapboxProperties mapboxProperties = new MapboxProperties();
        mapboxProperties.setEnabled(false);
        ReverseGeocodingService service = new ReverseGeocodingService(
                "https://nominatim.test/reverse",
                "lexho-local-redis-it",
                1,
                250,
                250,
                1,
                50,
                new SimpleMeterRegistry(),
                redisTemplate,
                new ObjectMapper(),
                mapboxProperties
        );

        RestTemplate restTemplate = mock(RestTemplate.class);
        ReverseGeocodeResponse providerResponse = new ReverseGeocodeResponse();
        providerResponse.formattedAddress = "1 MG Road, Bengaluru";
        providerResponse.city = "Bengaluru";
        providerResponse.state = "Karnataka";
        providerResponse.pincode = "560001";
        providerResponse.area = "Ashok Nagar";

        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(Map.of(
                        "display_name", providerResponse.formattedAddress,
                        "address", Map.of(
                                "city", providerResponse.city,
                                "state", providerResponse.state,
                                "postcode", providerResponse.pincode,
                                "suburb", providerResponse.area
                        )
                )));
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        ReverseGeocodeResponse first = service.reverse(12.9716, 77.5946);
        ReverseGeocodeResponse second = service.reverse(12.9716, 77.5946);

        String redisKey = "location:reverse-geocode:" + ReverseGeocodingService.cacheKey(12.9716, 77.5946);
        assertThat(first.city).isEqualTo("Bengaluru");
        assertThat(second.city).isEqualTo("Bengaluru");
        assertThat(redisTemplate.opsForValue().get(redisKey)).contains("Bengaluru");
        assertThat(redisTemplate.getExpire(redisKey)).isPositive();
    }

    private static void verifyServiceabilityCache(StringRedisTemplate redisTemplate) {
        ServiceablePincodeRepository repository = mock(ServiceablePincodeRepository.class);
        CustomerCurrentLocationRepository customerCurrentLocationRepository = mock(CustomerCurrentLocationRepository.class);
        GeoFenceValidationService geoFenceValidationService = mock(GeoFenceValidationService.class);
        CurrentPartnerLocationRepository currentPartnerLocationRepository = mock(CurrentPartnerLocationRepository.class);
        PartnerShiftRepository partnerShiftRepository = mock(PartnerShiftRepository.class);
        GeoEventPublisher geoEventPublisher = mock(GeoEventPublisher.class);
        RestrictedAreaOpsService restrictedAreaOpsService = mock(RestrictedAreaOpsService.class);

        when(geoFenceValidationService.resolveServiceability("110001", 28.6, 77.2, null))
                .thenReturn(GeoFenceValidationService.GeoResolution.serviceable(1L, 2L, "110001"));

        ServiceabilityServiceImpl service = new ServiceabilityServiceImpl(
                repository,
                customerCurrentLocationRepository,
                geoFenceValidationService,
                currentPartnerLocationRepository,
                partnerShiftRepository,
                geoEventPublisher,
                10,
                5.0d,
                1,
                18,
                22,
                false,
                new SimpleMeterRegistry(),
                redisTemplate,
                restrictedAreaOpsService,
                new ObjectMapper()
        );

        ServiceabilityCheckResponse first = service.resolve("110001", 28.6, 77.2);
        ServiceabilityCheckResponse second = service.resolve("110001", 28.6, 77.2);

        String redisKey = "location:serviceability:110001:2860000:7720000";
        assertThat(first.isServiceable()).isTrue();
        assertThat(second.isServiceable()).isTrue();
        assertThat(redisTemplate.opsForValue().get(redisKey)).contains("\"serviceable\":true");
        assertThat(redisTemplate.getExpire(redisKey)).isPositive();
    }

    private static void verifyNavigationRouteCache(StringRedisTemplate redisTemplate) {
        NavigationRouteCacheService service = new NavigationRouteCacheService(provider(redisTemplate), new ObjectMapper());
        NavigationRouteCacheService.CachedRoute route = new NavigationRouteCacheService.CachedRoute(
                4.2,
                12,
                "polyline",
                "MAPBOX",
                28.61,
                77.20,
                28.62,
                77.21,
                true,
                2,
                null
        );

        service.put("it:navigation:route", route, Duration.ofSeconds(45));

        assertThat(service.get("it:navigation:route")).isPresent();
        assertThat(redisTemplate.getExpire("it:navigation:route")).isPositive();
    }

    private static void verifyPartnerAppRedisBackedFlows(StringRedisTemplate redisTemplate) {
        PartnerGeoIndexService globalGeoIndex = new PartnerGeoIndexService(redisTemplate, false, 4);
        globalGeoIndex.upsertPartnerLocation(101L, 28.6100, 77.2000);
        globalGeoIndex.upsertPartnerLocation(102L, 28.6110, 77.2010);
        globalGeoIndex.upsertPartnerLocation(103L, 28.6120, 77.2020, true);

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearby = globalGeoIndex.findNearby(28.6100, 77.2000, 2.0d, 5);
        assertThat(nearby)
                .extracting(result -> result.getContent().getName())
                .contains("101", "102", "103");
        assertThat(globalGeoIndex.distanceKm(101L, 102L)).isPresent();
        assertThat(globalGeoIndex.findLastSeenEpochMs(101L)).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(PartnerGeoIndexService.PARTNER_GEO_ACTIVE_KEY, "101")).isNotNull();
        assertThat(redisTemplate.opsForZSet().score(PartnerGeoIndexService.PARTNER_GEO_HEARTBEAT_KEY, "103")).isNotNull();

        PartnerGeoIndexService shardedGeoIndex = new PartnerGeoIndexService(redisTemplate, true, 4);
        shardedGeoIndex.upsertPartnerLocation(201L, 28.6100, 77.2000);
        assertThat(shardedGeoIndex.findNearby(28.6100, 77.2000, 2.0d, 5))
                .extracting(result -> result.getContent().getName())
                .contains("201");
        assertThat(redisTemplate.opsForValue().get("dispatch:partners:geo:member-shard:201")).isNotBlank();

        TrackingLifecycleService trackingLifecycleService = new TrackingLifecycleService(
                redisTemplate,
                60.0d,
                7,
                30_000L,
                20_000L,
                5_000L,
                3_000L,
                2_000L,
                2_000L,
                30_000L,
                15_000L
        );
        trackingLifecycleService.persistState(101L, TrackingLifecycleState.EN_ROUTE, 5001L);
        assertThat(trackingLifecycleService.fetchState(101L)).isEqualTo(TrackingLifecycleState.EN_ROUTE);
        assertThat(redisTemplate.opsForValue().get("tracking:state:101")).isEqualTo("EN_ROUTE:5001");
        assertThat(redisTemplate.getExpire("tracking:state:101")).isPositive();

        NavigationEtaTrackingRegistry etaTrackingRegistry = new NavigationEtaTrackingRegistry(provider(redisTemplate));
        etaTrackingRegistry.register(9001L, Duration.ofSeconds(45));
        assertThat(etaTrackingRegistry.activeBookingIds(10)).contains(9001L);
        assertThat(redisTemplate.opsForZSet().score("navigation:live:bookings", "9001")).isNotNull();
        etaTrackingRegistry.unregister(9001L);
        assertThat(etaTrackingRegistry.activeBookingIds(10)).doesNotContain(9001L);
    }

    private static void verifyAdminAppRedisBackedFlows(StringRedisTemplate redisTemplate) {
        ScaleProfileService scaleProfileService = new ScaleProfileService(redisTemplate);
        Map<String, Object> applied = scaleProfileService.applyProfile("10k", false);
        Map<String, Object> current = scaleProfileService.current();

        assertThat(applied)
                .containsEntry("profile", "10k")
                .containsEntry("storage", "redis")
                .containsEntry("applied", true);
        assertThat(current)
                .containsEntry("activeProfile", "10k")
                .containsEntry("storage", "redis");
        assertThat(redisTemplate.opsForValue().get("ops:scale:active-profile")).isEqualTo("10k");
        assertThat(redisTemplate.<String, String>opsForHash().entries("ops:scale:config:10k"))
                .containsEntry("dispatch.geo.partition.enabled", "true")
                .containsEntry("geo.events.rabbit.partition.count", "16");

        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.dashboardStats()).thenReturn(Map.of("totalUsers", 12L));
        Map<String, Object> dashboard = new AdminMonitoringService(
                dashboardService,
                new SimpleMeterRegistry(),
                provider(null),
                provider(redisTemplate)
        ).dashboard();
        assertThat(dashboard)
                .containsEntry("status", "UP")
                .containsEntry("redisHealth", "UP")
                .containsEntry("databaseHealth", "DOWN")
                .containsEntry("totalUsers", 12L);

        AdminAuthProperties adminAuthProperties = new AdminAuthProperties();
        adminAuthProperties.getRateLimit().setKeyPrefix("it:admin-login:rl:");
        adminAuthProperties.getRateLimit().setMaxRequests(2);
        adminAuthProperties.getRateLimit().setWindowSeconds(60);

        AdminLoginRateLimiterService rateLimiterService =
                new AdminLoginRateLimiterService(redisTemplate, adminAuthProperties);
        rateLimiterService.validateAllowed("ADMIN@LEXHO.TEST");
        rateLimiterService.validateAllowed("admin@lexho.test");
        assertThatThrownBy(() -> rateLimiterService.validateAllowed("admin@lexho.test"))
                .isInstanceOf(TooManyRequestsException.class);

        String rateLimitKey = "it:admin-login:rl:admin@lexho.test";
        assertThat(redisTemplate.opsForValue().get(rateLimitKey)).isEqualTo("3");
        assertThat(redisTemplate.getExpire(rateLimitKey)).isPositive();
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }

    private record RedisSettings(String host, int port, String password) {
        private static final String DEFAULT_HOST = "localhost";
        private static final int DEFAULT_PORT = 6379;

        private static RedisSettings load() throws IOException {
            Map<String, String> dotEnv = loadDotEnv();
            String host = resolve("IT_REDIS_HOST", "REDIS_HOST", DEFAULT_HOST, dotEnv);
            String portValue = resolve("IT_REDIS_PORT", "REDIS_PORT", Integer.toString(DEFAULT_PORT), dotEnv);
            String password = resolve("IT_REDIS_PASSWORD", "REDIS_PASSWORD", "", dotEnv);
            return new RedisSettings(host, Integer.parseInt(portValue), password);
        }

        private static String resolve(String primaryKey,
                                      String secondaryKey,
                                      String defaultValue,
                                      Map<String, String> dotEnv) {
            String systemProperty = System.getProperty(primaryKey);
            if (hasText(systemProperty)) {
                return systemProperty;
            }

            String environmentValue = System.getenv(primaryKey);
            if (hasText(environmentValue)) {
                return environmentValue;
            }

            String primaryDotEnv = dotEnv.get(primaryKey);
            if (hasText(primaryDotEnv)) {
                return primaryDotEnv;
            }

            String secondarySystemProperty = System.getProperty(secondaryKey);
            if (hasText(secondarySystemProperty)) {
                return secondarySystemProperty;
            }

            String secondaryEnvironmentValue = System.getenv(secondaryKey);
            if (hasText(secondaryEnvironmentValue)) {
                return secondaryEnvironmentValue;
            }

            String secondaryDotEnv = dotEnv.get(secondaryKey);
            if (hasText(secondaryDotEnv)) {
                return secondaryDotEnv;
            }

            return defaultValue;
        }

        private static Map<String, String> loadDotEnv() throws IOException {
            Path dotEnvPath = null;
            for (Path candidate : List.of(Path.of(".env"), Path.of("..", ".env"))) {
                if (Files.exists(candidate)) {
                    dotEnvPath = candidate;
                    break;
                }
            }
            if (dotEnvPath == null) {
                return Map.of();
            }

            Map<String, String> values = new HashMap<>();
            List<String> lines = Files.readAllLines(dotEnvPath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
            return values;
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
