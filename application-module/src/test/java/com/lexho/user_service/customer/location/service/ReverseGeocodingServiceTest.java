package com.lexho.user_service.customer.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.map.config.MapboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseGeocodingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MapboxProperties mapboxProperties;
    private ReverseGeocodingService service;

    @BeforeEach
    void setUp() {
        mapboxProperties = new MapboxProperties();
        service = new ReverseGeocodingService(
                "https://nominatim.test/reverse", "test-agent", 100,
                1, 1, 2, 1, new SimpleMeterRegistry(), redisTemplate,
                objectMapper, mapboxProperties);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsCachedResponseWithoutCallingProviders() throws Exception {
        ReverseGeocodeResponse cached = response("Bengaluru", "Karnataka", "560001");
        when(valueOperations.get("location:reverse-geocode:1297160:7759460"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        ReverseGeocodeResponse result = service.reverse(12.9716, 77.5946);

        assertThat(result.city).isEqualTo("Bengaluru");
        verify(valueOperations).get("location:reverse-geocode:1297160:7759460");
    }

    @Test
    void parsesMapboxFeaturesAndWritesCache() {
        enableMapbox();
        Map<String, Object> feature = Map.of(
                "place_name", "  MG Road, Bengaluru  ",
                "text", " MG Road ",
                "context", List.of(
                        Map.of("id", "place.1", "text", "Bengaluru Dist."),
                        Map.of("id", "region.1", "text", "Karnataka"),
                        Map.of("id", "country.1", "text", "India"),
                        Map.of("id", "postcode.1", "text", "560 001"),
                        Map.of("id", "neighborhood.1", "text", "Central Dist")));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("features", List.of(feature)));

        ReverseGeocodeResponse result = service.reverse(12.9716, 77.5946);

        assertThat(result.formattedAddress).isEqualTo("MG Road, Bengaluru");
        assertThat(result.city).isEqualTo("Bengaluru District.");
        assertThat(result.area).isEqualTo("Central District");
        assertThat(result.pincode).isEqualTo("560001");
        verify(valueOperations).set(eq("location:reverse-geocode:1297160:7759460"),
                anyString(), eq(Duration.ofHours(48)));
    }

    @Test
    void fallsBackToNominatimAndUsesAlternativeAddressFields() {
        mapboxProperties.setEnabled(false);
        Map<String, Object> address = Map.of(
                "village", "  Mysuru Dist. ",
                "state", " Karnataka ",
                "country", " India ",
                "postcode", "570-001",
                "neighbourhood", " Central ",
                "road", " Palace Road ");
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "display_name", " Mysuru, Karnataka ",
                        "address", address)));

        ReverseGeocodeResponse result = service.reverse(12.3, 76.6);

        assertThat(result.formattedAddress).isEqualTo("Mysuru, Karnataka");
        assertThat(result.city).isEqualTo("Mysuru District.");
        assertThat(result.pincode).isEqualTo("570001");
        assertThat(result.area).isEqualTo("Central");
        assertThat(result.landmark).isEqualTo("Palace Road");
    }

    @Test
    void providerAndCacheFailuresReturnEmptyResponseWithoutBreakingFlow() {
        when(valueOperations.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new IllegalStateException("provider down"));
        doThrow(new IllegalStateException("redis write down")).when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        ReverseGeocodeResponse result = service.reverse(1.0, 2.0);

        assertThat(result.formattedAddress).isNull();
        assertThat(result.city).isNull();
    }

    @Test
    void fallbackReturnsCacheOrEmptyResponse() throws Exception {
        ReverseGeocodeResponse cached = response("Delhi", "Delhi", "110001");
        when(valueOperations.get(anyString()))
                .thenReturn(objectMapper.writeValueAsString(cached), null);

        ReverseGeocodeResponse first = ReflectionTestUtils.invokeMethod(
                service, "reverseFallback", 28.6, 77.2, new RuntimeException());
        ReverseGeocodeResponse second = ReflectionTestUtils.invokeMethod(
                service, "reverseFallback", 28.6, 77.2, new RuntimeException());

        assertThat(first.city).isEqualTo("Delhi");
        assertThat(second.city).isNull();
    }

    @Test
    void staticNormalizationCoversNullBlankValidAndInvalidValues() {
        assertThat(ReverseGeocodingService.cacheKey(12.345678, 77.123456))
                .isEqualTo("1234568:7712346");
        assertThat(ReverseGeocodingService.normalizePincode(null)).isEmpty();
        assertThat(ReverseGeocodingService.normalizePincode("PIN 560 001")).isEqualTo("560001");
        assertThat(ReverseGeocodingService.normalizePincode("123")).isEmpty();
        assertThat(ReverseGeocodingService.normalizeAddressText(null)).isNull();
        assertThat(ReverseGeocodingService.normalizeAddressText("   ")).isNull();
        assertThat(ReverseGeocodingService.normalizeAddressText("  New   Delhi ")).isEqualTo("New Delhi");
        assertThat(ReverseGeocodingService.normalizeLocality("South Dist.")).isEqualTo("South District.");
    }

    @Test
    void retryHelperRetriesRuntimeFailuresAndPropagatesFinalError() {
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        Object supplier = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{nestedSupplierType()},
                (proxy, method, args) -> {
                    if (attempts.incrementAndGet() < 2) {
                        throw new IllegalStateException("temporary");
                    }
                    return "ok";
                });

        assertThat((String) ReflectionTestUtils.invokeMethod(service, "executeWithRetry", supplier))
                .isEqualTo("ok");
        assertThat(attempts).hasValue(2);

        Object failing = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{nestedSupplierType()},
                (proxy, method, args) -> {
                    throw new IllegalArgumentException("failed");
        });
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "executeWithRetry", failing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("failed");
    }

    private void enableMapbox() {
        mapboxProperties.setEnabled(true);
        mapboxProperties.setAccessToken(" token ");
        mapboxProperties.setGeocodingUrl("https://mapbox.test");
    }

    private ReverseGeocodeResponse response(String city, String state, String pincode) {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse();
        response.city = city;
        response.state = state;
        response.pincode = pincode;
        return response;
    }

    private Class<?> nestedSupplierType() {
        return java.util.Arrays.stream(ReverseGeocodingService.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("ThrowingSupplier"))
                .findFirst()
                .orElseThrow();
    }
}
