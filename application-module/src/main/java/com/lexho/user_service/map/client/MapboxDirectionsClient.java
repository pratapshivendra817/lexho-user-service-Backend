package com.lexho.user_service.map.client;

import com.lexho.user_service.map.config.MapboxProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MapboxDirectionsClient {
    private static final String DRIVING_TRAFFIC_PROFILE = "driving-traffic";

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
            RouteSelection selection = selectBestRoute(routes, profile);
            RouteResult best = selection.best();
            if (best == null) {
                return null;
            }
            return new RouteResult(
                    best.distanceKm(),
                    best.durationMinutes(),
                    best.polyline(),
                    best.provider(),
                    best.trafficAware(),
                    Math.max(selection.alternativesConsidered(), 1)
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

    private RouteSelection selectBestRoute(List<?> routes, String profile) {
        RouteResult best = null;
        int alternativesConsidered = 0;
        for (Object candidate : routes) {
            RouteResult current = routeResult(candidate, profile);
            if (current == null) {
                continue;
            }
            alternativesConsidered++;
            if (best == null || isBetter(current, best)) {
                best = current;
            }
        }
        return new RouteSelection(best, alternativesConsidered);
    }

    private RouteResult routeResult(Object candidate, String profile) {
        if (!(candidate instanceof Map route)) {
            return null;
        }
        Double distanceMeters = toDouble(route.get("distance"));
        Double durationSeconds = toDouble(route.get("duration"));
        if (distanceMeters == null || durationSeconds == null) {
            return null;
        }
        String polyline = route.get("geometry") instanceof String value ? value : null;
        return new RouteResult(
                round2(distanceMeters / 1000d),
                (int) Math.ceil(durationSeconds / 60d),
                polyline,
                "MAPBOX",
                DRIVING_TRAFFIC_PROFILE.equals(profile),
                1
        );
    }

    private String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return DRIVING_TRAFFIC_PROFILE;
        }
        String normalized = profile.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "driving", DRIVING_TRAFFIC_PROFILE, "walking", "cycling" -> normalized;
            default -> DRIVING_TRAFFIC_PROFILE;
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

    private record RouteSelection(RouteResult best, int alternativesConsidered) {
    }
}
