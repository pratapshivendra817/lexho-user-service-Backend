package com.lexho.user_service.customer.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.map.config.MapboxProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ReverseGeocodingService implements ReverseGeocodeService {

    private final RestTemplate restTemplate;
    private final String reverseGeocodeUrl;
    private final String userAgent;
    private final Timer reverseGeocodeLatency;
    private final Counter reverseGeocodeCallCounter;
    private final Counter reverseGeocodeCacheHitCounter;
    private final Counter reverseGeocodeCacheMissCounter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MapboxProperties mapboxProperties;
    private final Duration cacheTtl;
    private final int mapboxMaxAttempts;
    private final long mapboxBackoffMs;

    public ReverseGeocodingService(
            @Value("${app.geocode.reverse-url:https://nominatim.openstreetmap.org/reverse}") String reverseGeocodeUrl,
            @Value("${app.geocode.user-agent:lexho-user-service/1.0}") String userAgent,
            @Value("${app.cache.reverse-geocode.ttl-hours:24}") long cacheTtlHours,
            @Value("${app.mapbox.reverse.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${app.mapbox.reverse.read-timeout-ms:1500}") int readTimeoutMs,
            @Value("${app.mapbox.reverse.max-attempts:3}") int mapboxMaxAttempts,
            @Value("${app.mapbox.reverse.backoff-ms:200}") long mapboxBackoffMs,
            MeterRegistry meterRegistry,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MapboxProperties mapboxProperties) {
        this.reverseGeocodeUrl = reverseGeocodeUrl;
        this.userAgent = userAgent;
        this.cacheTtl = Duration.ofHours(Math.max(1L, Math.min(48L, cacheTtlHours)));
        this.reverseGeocodeLatency = Timer.builder("location.reverse_geocode.latency").register(meterRegistry);
        this.reverseGeocodeCallCounter = Counter.builder("location.reverse_geocode.calls").register(meterRegistry);
        this.reverseGeocodeCacheHitCounter = Counter.builder("location.reverse_geocode.cache_hit").register(meterRegistry);
        this.reverseGeocodeCacheMissCounter = Counter.builder("location.reverse_geocode.cache_miss").register(meterRegistry);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.mapboxProperties = mapboxProperties;
        this.mapboxMaxAttempts = Math.max(1, mapboxMaxAttempts);
        this.mapboxBackoffMs = Math.max(50L, mapboxBackoffMs);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(200, connectTimeoutMs));
        requestFactory.setReadTimeout(Math.max(300, readTimeoutMs));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    @Retry(name = "reverseGeocodeProvider", fallbackMethod = "reverseFallback")
    @CircuitBreaker(name = "reverseGeocodeProvider", fallbackMethod = "reverseFallback")
    public ReverseGeocodeResponse reverse(double latitude, double longitude) {
        String cacheKey = "location:reverse-geocode:" + cacheKey(latitude, longitude);
        ReverseGeocodeResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            reverseGeocodeCacheHitCounter.increment();
            return cached;
        }
        reverseGeocodeCacheMissCounter.increment();

        return reverseGeocodeLatency.record(() -> {
            reverseGeocodeCallCounter.increment();
            ReverseGeocodeResponse response = fetchUsingMapbox(latitude, longitude);
            if (isEmpty(response)) {
                response = fetchUsingNominatim(latitude, longitude);
            }
            writeToCache(cacheKey, response);
            return response;
        });
    }

    private ReverseGeocodeResponse reverseFallback(double latitude, double longitude, Throwable throwable) {
        String cacheKey = "location:reverse-geocode:" + cacheKey(latitude, longitude);
        ReverseGeocodeResponse cached = readFromCache(cacheKey);
        return cached == null ? new ReverseGeocodeResponse() : cached;
    }

    private ReverseGeocodeResponse fetchUsingMapbox(double latitude, double longitude) {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse();
        try {
            if (!mapboxProperties.isEnabled()
                    || mapboxProperties.getAccessToken() == null
                    || mapboxProperties.getAccessToken().isBlank()) {
                return response;
            }
            String token = URLEncoder.encode(mapboxProperties.getAccessToken().trim(), StandardCharsets.UTF_8);
            String url = mapboxProperties.getGeocodingUrl()
                    + "/" + longitude + "," + latitude + ".json"
                    + "?access_token=" + token + "&types=address,postcode,place,locality,neighborhood";
            Map body = executeWithRetry(() -> restTemplate.getForObject(url, Map.class));
            if (body == null || !(body.get("features") instanceof List<?> features) || features.isEmpty()) {
                return response;
            }

            Object firstObj = features.get(0);
            if (firstObj instanceof Map firstFeature) {
                Object placeName = firstFeature.get("place_name");
                if (placeName instanceof String s) {
                    response.formattedAddress = normalizeAddressText(s);
                }
                Object text = firstFeature.get("text");
                if (text instanceof String s) {
                    response.landmark = normalizeAddressText(s);
                }
                Object contextObj = firstFeature.get("context");
                if (contextObj instanceof List<?> contexts) {
                    for (Object contextItem : contexts) {
                        if (!(contextItem instanceof Map context)) {
                            continue;
                        }
                        String id = context.get("id") instanceof String v ? v : "";
                        String value = context.get("text") instanceof String v ? v : null;
                        if (id.startsWith("place.")) {
                            response.city = normalizeLocality(value);
                        } else if (id.startsWith("region.")) {
                            response.state = normalizeAddressText(value);
                        } else if (id.startsWith("country.")) {
                            response.country = normalizeAddressText(value);
                        } else if (id.startsWith("postcode.")) {
                            response.pincode = normalizePincode(value);
                        } else if (id.startsWith("locality.") || id.startsWith("neighborhood.")) {
                            response.area = normalizeLocality(value);
                        }
                    }
                }
            }
            if (response.country == null) {
                response.country = "India";
            }
            return response;
        } catch (Exception ignored) {
            return response;
        }
    }

    private ReverseGeocodeResponse fetchUsingNominatim(double latitude, double longitude) {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse();
        try {
            String url = reverseGeocodeUrl
                    + "?format=jsonv2&addressdetails=1"
                    + "&lat=" + latitude
                    + "&lon=" + longitude;
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", userAgent);
            ResponseEntity<Map> apiResponse = executeWithRetry(
                    () -> restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
            );
            Map body = apiResponse.getBody();
            if (body == null) {
                return response;
            }
            Object displayName = body.get("display_name");
            if (displayName instanceof String s) {
                response.formattedAddress = normalizeAddressText(s);
            }
            Object addressObj = body.get("address");
            if (addressObj instanceof Map address) {
                response.city = normalizeLocality(firstString(address, "city", "town", "village", "hamlet"));
                response.state = normalizeAddressText(firstString(address, "state"));
                response.country = normalizeAddressText(firstString(address, "country"));
                response.pincode = normalizePincode(firstString(address, "postcode"));
                response.area = normalizeLocality(firstString(
                        address,
                        "suburb",
                        "neighbourhood",
                        "quarter",
                        "residential",
                        "city_district",
                        "state_district",
                        "county"
                ));
                response.landmark = normalizeAddressText(firstString(
                        address,
                        "road",
                        "amenity",
                        "building",
                        "house_name",
                        "house_number"
                ));
            }
            return response;
        } catch (Exception ignored) {
            return response;
        }
    }

    private ReverseGeocodeResponse readFromCache(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, ReverseGeocodeResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeToCache(String key, ReverseGeocodeResponse response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), cacheTtl);
        } catch (Exception ignored) {
            // cache failures should not fail location flow
        }
    }

    private boolean isEmpty(ReverseGeocodeResponse response) {
        return response == null
                || (response.formattedAddress == null
                && response.city == null
                && response.state == null
                && response.pincode == null);
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

    public static String cacheKey(double latitude, double longitude) {
        long roundedLat = Math.round(latitude * 100000.0d);
        long roundedLng = Math.round(longitude * 100000.0d);
        return roundedLat + ":" + roundedLng;
    }

    public static String normalizePincode(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 6 ? digits : "";
    }

    public static String normalizeAddressText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeLocality(String raw) {
        String normalized = normalizeAddressText(raw);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\bDist\\.?\\b", "District");
    }

    private <T> T executeWithRetry(ThrowingSupplier<T> supplier) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= mapboxMaxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < mapboxMaxAttempts) {
                    try {
                        Thread.sleep(mapboxBackoffMs * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
        throw lastException == null ? new IllegalStateException("Map provider call failed") : lastException;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
