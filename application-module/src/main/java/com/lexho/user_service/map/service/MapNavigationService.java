package com.lexho.user_service.map.service;

import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.dto.RouteEtaRequest;
import com.lexho.user_service.map.dto.RouteEtaResponse;
import com.lexho.user_service.util.GeoDistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MapNavigationService {

    private final MapboxProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public MapNavigationService(MapboxProperties properties) {
        this.properties = properties;
    }

    public RouteEtaResponse estimateRoute(RouteEtaRequest request) {
        String profile = normalizeProfile(request.profile);
        if (properties.isEnabled() && properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
            RouteEtaResponse fromMapbox = fetchFromMapbox(request, profile);
            if (fromMapbox != null) {
                return fromMapbox;
            }
        }
        return fallbackByHaversine(request, profile);
    }

    private RouteEtaResponse fetchFromMapbox(RouteEtaRequest request, String profile) {
        try {
            String coordinates = request.originLongitude + "," + request.originLatitude + ";"
                    + request.destinationLongitude + "," + request.destinationLatitude;
            String url = properties.getDirectionsUrl() + "/" + profile + "/" + coordinates
                    + "?access_token=" + properties.getAccessToken().trim()
                    + "&overview=simplified&geometries=geojson";
            Map body = restTemplate.getForObject(url, Map.class);
            if (body == null || !(body.get("routes") instanceof List<?> routes) || routes.isEmpty()) {
                return null;
            }
            Object first = routes.get(0);
            if (!(first instanceof Map routeMap)) {
                return null;
            }
            Double distanceMeters = toDouble(routeMap.get("distance"));
            Double durationSecondsRaw = toDouble(routeMap.get("duration"));
            RouteEtaResponse response = new RouteEtaResponse();
            response.profile = profile;
            response.provider = "MAPBOX";
            response.distanceKm = distanceMeters == null ? null : round2(distanceMeters / 1000d);
            response.durationSeconds = durationSecondsRaw == null ? null : Math.round(durationSecondsRaw);
            response.etaMinutes = response.durationSeconds == null ? null : (int) Math.ceil(response.durationSeconds / 60d);
            response.geometry = routeMap.get("geometry") == null ? null : String.valueOf(routeMap.get("geometry"));
            return response;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private RouteEtaResponse fallbackByHaversine(RouteEtaRequest request, String profile) {
        double distanceKm = GeoDistanceCalculator.haversineKm(request.originLatitude, request.originLongitude,
                request.destinationLatitude, request.destinationLongitude);
        double avgSpeedKmph = switch (profile) {
            case "walking" -> 5d;
            case "cycling" -> 15d;
            default -> 28d;
        };
        long durationSeconds = Math.max(60L, Math.round((distanceKm / avgSpeedKmph) * 3600d));
        RouteEtaResponse response = new RouteEtaResponse();
        response.profile = profile;
        response.provider = "HAVERSINE_FALLBACK";
        response.distanceKm = round2(distanceKm);
        response.durationSeconds = durationSeconds;
        response.etaMinutes = (int) Math.ceil(durationSeconds / 60d);
        response.geometry = null;
        return response;
    }

    private String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return "driving";
        }
        String normalized = profile.trim().toLowerCase(Locale.ROOT);
        if ("walking".equals(normalized) || "cycling".equals(normalized)) {
            return normalized;
        }
        return "driving";
    }

    private Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
