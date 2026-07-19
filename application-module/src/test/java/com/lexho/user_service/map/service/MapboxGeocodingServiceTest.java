package com.lexho.user_service.map.service;

import com.lexho.user_service.map.config.MapboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MapboxGeocodingServiceTest {
    private MapboxProperties properties;
    private RestTemplate restTemplate;
    private MapboxGeocodingService service;

    @BeforeEach
    void setUp() {
        properties = new MapboxProperties();
        restTemplate = mock(RestTemplate.class);
        service = new MapboxGeocodingService(properties);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void disabledOrMissingTokenReturnsEmptyWithoutHttpCall() {
        assertTrue(service.forwardGeocode("Delhi", 5).results.isEmpty());
        properties.setEnabled(true);
        properties.setAccessToken(" ");
        assertTrue(service.forwardGeocode("Delhi", 5).results.isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void parsesFeaturesCoordinatesAndContext() {
        properties.setEnabled(true); properties.setAccessToken("token");
        Map<String, Object> feature = Map.of(
                "place_name", "Connaught Place",
                "center", List.of("77.21", 28.63),
                "context", List.of(
                        Map.of("id", "place.delhi", "text", "Delhi"),
                        Map.of("id", "region.delhi", "text", "Delhi NCR"),
                        Map.of("id", "country.india", "text", "India"),
                        Map.of("id", "postcode.110001", "text", "110001")));
        when(restTemplate.getForObject(contains("Delhi+NCR.json"), eq(Map.class)))
                .thenReturn(Map.of("features", List.of(feature, "invalid")));

        var result = service.forwardGeocode(" Delhi NCR ", 99);

        assertEquals(1, result.results.size());
        assertEquals(77.21, result.results.get(0).longitude);
        assertEquals("110001", result.results.get(0).pincode);
        verify(restTemplate).getForObject(contains("limit=10"), eq(Map.class));
    }

    @Test
    void malformedOrMissingFeaturesReturnSafeResults() {
        properties.setEnabled(true); properties.setAccessToken("token");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null, Map.of("features", "bad"),
                Map.of("features", List.of(Map.of("center", List.of("bad", 1)))));
        assertTrue(service.forwardGeocode(null, null).results.isEmpty());
        assertTrue(service.forwardGeocode("x", 0).results.isEmpty());
        var parsed = service.forwardGeocode("x", 1);
        assertNull(parsed.results.get(0).longitude);
        assertEquals(1d, parsed.results.get(0).latitude);
    }
}
