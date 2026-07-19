package com.lexho.user_service.map.service;

import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.dto.RouteEtaRequest;
import com.lexho.user_service.map.dto.RouteEtaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapNavigationServiceTest {

    private MapboxProperties properties;
    private MapNavigationService service;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        properties = new MapboxProperties();
        service = new MapNavigationService(properties);
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void disabledMapboxUsesHaversineForEveryTravelProfile() {
        RouteEtaResponse driving = service.estimateRoute(request(null));
        RouteEtaResponse walking = service.estimateRoute(request(" WALKING "));
        RouteEtaResponse cycling = service.estimateRoute(request("cycling"));

        assertThat(driving.profile).isEqualTo("driving");
        assertThat(walking.durationSeconds).isGreaterThan(cycling.durationSeconds);
        assertThat(cycling.durationSeconds).isGreaterThan(driving.durationSeconds);
        assertThat(driving.provider).isEqualTo("HAVERSINE_FALLBACK");
        assertThat(driving.distanceKm).isPositive();
        assertThat(driving.geometry).isNull();
    }

    @Test
    void mapboxResponseMapsNumericAndStringRouteValues() {
        enableMapbox();
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of(
                "routes", List.of(Map.of(
                        "distance", "1250.5",
                        "duration", 125.2,
                        "geometry", Map.of("type", "LineString")))));

        RouteEtaResponse result = service.estimateRoute(request("unsupported"));

        assertThat(result.provider).isEqualTo("MAPBOX");
        assertThat(result.profile).isEqualTo("driving");
        assertThat(result.distanceKm).isEqualTo(1.25);
        assertThat(result.durationSeconds).isEqualTo(125L);
        assertThat(result.etaMinutes).isEqualTo(3);
        assertThat(result.geometry).contains("LineString");
    }

    @Test
    void malformedEmptyAndFailedMapboxResponsesFallBack() {
        enableMapbox();
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(null)
                .thenReturn(Map.of("routes", List.of()))
                .thenReturn(Map.of("routes", List.of("invalid")))
                .thenThrow(new IllegalStateException("network down"));

        for (int i = 0; i < 4; i++) {
            assertThat(service.estimateRoute(request("driving")).provider)
                    .isEqualTo("HAVERSINE_FALLBACK");
        }
    }

    @Test
    void mapboxRouteWithInvalidNumbersReturnsNullableMetrics() {
        enableMapbox();
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of(
                "routes", List.of(Map.of("distance", "bad", "duration", new Object()))));

        RouteEtaResponse result = service.estimateRoute(request("driving"));

        assertThat(result.provider).isEqualTo("MAPBOX");
        assertThat(result.distanceKm).isNull();
        assertThat(result.durationSeconds).isNull();
        assertThat(result.etaMinutes).isNull();
        assertThat(result.geometry).isNull();
    }

    private void enableMapbox() {
        properties.setEnabled(true);
        properties.setAccessToken(" token ");
        properties.setDirectionsUrl("https://mapbox.test");
    }

    private RouteEtaRequest request(String profile) {
        RouteEtaRequest request = new RouteEtaRequest();
        request.originLatitude = 28.6139;
        request.originLongitude = 77.2090;
        request.destinationLatitude = 28.7041;
        request.destinationLongitude = 77.1025;
        request.profile = profile;
        return request;
    }
}
