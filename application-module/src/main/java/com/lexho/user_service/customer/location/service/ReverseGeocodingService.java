package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ReverseGeocodingService implements ReverseGeocodeService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String reverseGeocodeUrl;
    private final String userAgent;

    public ReverseGeocodingService(
            @Value("${app.geocode.reverse-url:https://nominatim.openstreetmap.org/reverse}") String reverseGeocodeUrl,
            @Value("${app.geocode.user-agent:lexho-user-service/1.0}") String userAgent) {
        this.reverseGeocodeUrl = reverseGeocodeUrl;
        this.userAgent = userAgent;
    }

    @Override
    public ReverseGeocodeResponse reverse(double latitude, double longitude) {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse();
        try {
            String url = reverseGeocodeUrl
                    + "?format=jsonv2&addressdetails=1"
                    + "&lat=" + latitude
                    + "&lon=" + longitude;
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", userAgent);
            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = apiResponse.getBody();
            if (body == null) {
                return response;
            }
            Object displayName = body.get("display_name");
            if (displayName instanceof String s) {
                response.formattedAddress = s;
            }
            Object addressObj = body.get("address");
            if (addressObj instanceof Map address) {
                response.city = firstString(address, "city", "town", "village", "hamlet");
                response.state = firstString(address, "state");
                response.pincode = firstString(address, "postcode");
            }
            return response;
        } catch (Exception ignored) {
            return response;
        }
    }

    private String firstString(Map map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }
}
