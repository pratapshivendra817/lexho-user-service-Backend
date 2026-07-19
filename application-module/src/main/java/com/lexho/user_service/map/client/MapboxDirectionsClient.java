package com.lexho.user_service.map.client;

import com.lexho.user_service.map.config.MapboxProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MapboxDirectionsClient {

    private final MapboxProperties properties;
    private RestTemplate restTemplate = new RestTemplate();

    public MapboxDirectionsClient(MapboxProperties properties) {
        this.properties = properties;
    }

    public RouteResult fetchBestRoute(double originLatitude,
                                      double originLongitude,
                                      double destinationLatitude,
                                      double destinationLongitude) {
        if (!properties.isEnabled() || properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            return null;
        }
        try {
            String profile = normalizeProfile(properties.getNavigationProfile());
            String coordinates = originLongitude + "," + originLatitude + ";"
                    + destinationLongitude + "," + destinationLatitude;
            String url = properties.getDirectionsUrl() + "/" + profile + "/" + coordinates
                    + "?access_token=" + properties.getAccessToken().trim()
                    + "&alternatives=true"
                    + "&overview=full"
                    + "&steps=false"
                    + "&annotations=duration,distance"
                    + "&geometries=polyline";
            Map body = restTemplate.getForObject(url, Map.class);
            if (body == null || !(body.get("routes") instanceof List<?> routes) || routes.isEmpty()) {
                return null;
            }
            RouteResult best = null;
            int alternativesConsidered = 0;
            for (Object candidate : routes) {
                if (!(candidate instanceof Map route)) {
                    continue;
                }
                Double distanceMeters = toDouble(route.get("distance"));
                Double durationSeconds = toDouble(route.get("duration"));
                String polyline = route.get("geometry") instanceof String value ? value : null;
                if (distanceMeters == null || durationSeconds == null) {
                    continue;
                }
                alternativesConsidered++;
                RouteResult current = new RouteResult(
                        round2(distanceMeters / 1000d),
                        (int) Math.ceil(durationSeconds / 60d),
                        polyline,
                        "MAPBOX",
                        "driving-traffic".equals(profile),
                        1
                );
                if (best == null || isBetter(current, best)) {
                    best = current;
                }
            }
            if (best == null) {
                return null;
            }
            return new RouteResult(
                    best.distanceKm(),
                    best.durationMinutes(),
                    best.polyline(),
                    best.provider(),
                    best.trafficAware(),
                    Math.max(alternativesConsidered, 1)
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isBetter(RouteResult candidate, RouteResult currentBest) {
        int candidateMinutes = candidate.durationMinutes() == null ? Integer.MAX_VALUE : candidate.durationMinutes();
        int currentBestMinutes = currentBest.durationMinutes() == null ? Integer.MAX_VALUE : currentBest.durationMinutes();
        if (candidateMinutes != currentBestMinutes) {
            return candidateMinutes < currentBestMinutes;
        }
        double candidateDistance = candidate.distanceKm() == null ? Double.MAX_VALUE : candidate.distanceKm();
        double currentBestDistance = currentBest.distanceKm() == null ? Double.MAX_VALUE : currentBest.distanceKm();
        return candidateDistance < currentBestDistance;
    }

    private String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return "driving-traffic";
        }
        String normalized = profile.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "driving", "driving-traffic", "walking", "cycling" -> normalized;
            default -> "driving-traffic";
        };
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }

    public record RouteResult(
            Double distanceKm,
            Integer durationMinutes,
            String polyline,
            String provider,
            Boolean trafficAware,
            Integer alternativesConsidered
    ) {
    }
}
