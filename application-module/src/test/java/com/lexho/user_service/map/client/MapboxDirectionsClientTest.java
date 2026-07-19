package com.lexho.user_service.map.client;

import com.lexho.user_service.map.config.MapboxProperties;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MapboxDirectionsClientTest {

    private MapboxProperties properties;
    private MapboxDirectionsClient client;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        properties = new MapboxProperties();
        client = new MapboxDirectionsClient(properties);
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    @Test
    void disabledClientReturnsNullWithoutHttpCall() {
        assertThat(client.fetchBestRoute(28.61, 77.20, 28.62, 77.21)).isNull();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void routeResponseMapsDistanceDurationAndPolyline() {
        properties.setEnabled(true);
        properties.setAccessToken(" token ");
        properties.setDirectionsUrl("https://mapbox.test");
        properties.setNavigationProfile("driving-traffic");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of(
                "routes", List.of(Map.of(
                        "distance", 4200.0,
                        "duration", 721.0,
                        "geometry", "encoded-polyline"
                ))
        ));

        MapboxDirectionsClient.RouteResult result = client.fetchBestRoute(28.61, 77.20, 28.62, 77.21);

        assertThat(result).isNotNull();
        assertThat(result.distanceKm()).isEqualTo(4.2);
        assertThat(result.durationMinutes()).isEqualTo(13);
        assertThat(result.polyline()).isEqualTo("encoded-polyline");
        assertThat(result.provider()).isEqualTo("MAPBOX");
        assertThat(result.trafficAware()).isTrue();
        assertThat(result.alternativesConsidered()).isEqualTo(1);
    }

    @Test
    void choosesFastestAlternative() {
        properties.setEnabled(true);
        properties.setAccessToken("token");
        properties.setDirectionsUrl("https://mapbox.test");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of(
                "routes", List.of(
                        Map.of("distance", 5100.0, "duration", 900.0, "geometry", "slow"),
                        Map.of("distance", 5600.0, "duration", 660.0, "geometry", "fast")
                )
        ));

        MapboxDirectionsClient.RouteResult result = client.fetchBestRoute(28.61, 77.20, 28.62, 77.21);

        assertThat(result).isNotNull();
        assertThat(result.polyline()).isEqualTo("fast");
        assertThat(result.durationMinutes()).isEqualTo(11);
        assertThat(result.alternativesConsidered()).isEqualTo(2);
    }

    @Test
    void malformedResponseReturnsNull() {
        properties.setEnabled(true);
        properties.setAccessToken("token");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("routes", List.of(Map.of("distance", "bad"))))
                .thenThrow(new IllegalStateException("timeout"));

        assertThat(client.fetchBestRoute(28.61, 77.20, 28.62, 77.21)).isNull();
        assertThat(client.fetchBestRoute(28.61, 77.20, 28.62, 77.21)).isNull();
    }
}
