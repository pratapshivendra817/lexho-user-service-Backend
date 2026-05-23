package com.lexho.user_service.map.service;

import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.dto.MapboxGeocodeItem;
import com.lexho.user_service.map.dto.MapboxGeocodeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class MapboxGeocodingService {

    private final MapboxProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public MapboxGeocodingService(MapboxProperties properties) {
        this.properties = properties;
    }

    public MapboxGeocodeResponse forwardGeocode(String query, Integer limit) {
        MapboxGeocodeResponse response = new MapboxGeocodeResponse();
        if (!properties.isEnabled() || properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            return response;
        }
        String encodedQuery = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8);
        String url = properties.getGeocodingUrl() + "/" + encodedQuery + ".json?access_token="
                + properties.getAccessToken().trim() + "&limit=" + (limit == null ? 5 : Math.max(1, Math.min(limit, 10)));
        Map body = restTemplate.getForObject(url, Map.class);
        if (body == null || !(body.get("features") instanceof List<?> features)) {
            return response;
        }
        for (Object featureObj : features) {
            if (!(featureObj instanceof Map feature)) {
                continue;
            }
            MapboxGeocodeItem item = new MapboxGeocodeItem();
            Object placeName = feature.get("place_name");
            if (placeName instanceof String s) {
                item.placeName = s;
            }
            Object centerObj = feature.get("center");
            if (centerObj instanceof List<?> center && center.size() >= 2) {
                item.longitude = toDouble(center.get(0));
                item.latitude = toDouble(center.get(1));
            }
            Object contextObj = feature.get("context");
            if (contextObj instanceof List<?> contexts) {
                for (Object contextItem : contexts) {
                    if (!(contextItem instanceof Map context)) {
                        continue;
                    }
                    String id = context.get("id") instanceof String v ? v : "";
                    String text = context.get("text") instanceof String v ? v : null;
                    if (id.startsWith("place.")) {
                        item.city = text;
                    } else if (id.startsWith("region.")) {
                        item.state = text;
                    } else if (id.startsWith("country.")) {
                        item.country = text;
                    } else if (id.startsWith("postcode.")) {
                        item.pincode = text;
                    }
                }
            }
            response.results.add(item);
        }
        return response;
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
}
