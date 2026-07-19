package com.lexho.user_service.customer.location.service.impl;

import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.service.RestrictedAreaOpsService;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.repository.ServiceablePincodeRepository;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServiceabilityServiceImpl implements ServiceabilityService {
    private final ServiceablePincodeRepository repository;
    private final CustomerCurrentLocationRepository customerCurrentLocationRepository;
    private final GeoFenceValidationService geoFenceValidationService;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final GeoEventPublisher geoEventPublisher;
    private final long cacheTtlMs;
    private final double dynamicPartnerRadiusKm;
    private final int dynamicMinActivePartners;
    private final int peakHourStart;
    private final int peakHourEnd;
    private final boolean dynamicCapacityEnabled;
    private final Counter serviceabilityFailureCounter;
    private final Counter unsupportedAreaCounter;
    private final Counter serviceabilityCacheHitCounter;
    private final Counter serviceabilityCacheMissCounter;
    private final StringRedisTemplate redisTemplate;
    private final RestrictedAreaOpsService restrictedAreaOpsService;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ServiceabilityServiceImpl(ServiceablePincodeRepository repository,
                                     CustomerCurrentLocationRepository customerCurrentLocationRepository,
                                     GeoFenceValidationService geoFenceValidationService,
                                     CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                     PartnerShiftRepository partnerShiftRepository,
                                     GeoEventPublisher geoEventPublisher,
                                     @Value("${app.cache.serviceability.ttl-minutes:10}") long cacheTtlMinutes,
                                     @Value("${app.serviceability.dynamic.partner-radius-km:5}") double dynamicPartnerRadiusKm,
                                     @Value("${app.serviceability.dynamic.min-active-partners:1}") int dynamicMinActivePartners,
                                     @Value("${app.serviceability.dynamic.peak-hour-start:18}") int peakHourStart,
                                     @Value("${app.serviceability.dynamic.peak-hour-end:22}") int peakHourEnd,
                                     @Value("${app.serviceability.dynamic.enabled:false}") boolean dynamicCapacityEnabled,
                                     MeterRegistry meterRegistry,
                                     StringRedisTemplate redisTemplate,
                                     RestrictedAreaOpsService restrictedAreaOpsService,
                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.customerCurrentLocationRepository = customerCurrentLocationRepository;
        this.geoFenceValidationService = geoFenceValidationService;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.geoEventPublisher = geoEventPublisher;
        this.cacheTtlMs = Math.max(5L, Math.min(15L, cacheTtlMinutes)) * 60_000L;
        this.dynamicPartnerRadiusKm = Math.max(1.0d, Math.min(15.0d, dynamicPartnerRadiusKm));
        this.dynamicMinActivePartners = Math.max(1, Math.min(20, dynamicMinActivePartners));
        this.peakHourStart = Math.max(0, Math.min(23, peakHourStart));
        this.peakHourEnd = Math.max(0, Math.min(23, peakHourEnd));
        this.dynamicCapacityEnabled = dynamicCapacityEnabled;
        this.serviceabilityFailureCounter = Counter.builder("location.serviceability.failures").register(meterRegistry);
        this.unsupportedAreaCounter = Counter.builder("location.serviceability.unsupported_area_attempts").register(meterRegistry);
        this.serviceabilityCacheHitCounter = Counter.builder("location.serviceability.cache_hit").register(meterRegistry);
        this.serviceabilityCacheMissCounter = Counter.builder("location.serviceability.cache_miss").register(meterRegistry);
        this.redisTemplate = redisTemplate;
        this.restrictedAreaOpsService = restrictedAreaOpsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isServiceable(String pincode) {
        return repository.existsByPincodeAndActiveTrue(pincode == null ? "" : pincode.trim());
    }

    @Override
    public ServiceabilityCheckResponse resolve(String pincode, Double latitude, Double longitude) {
        String normalizedPincode = normalizePincode(pincode);
        String cacheKey = "location:serviceability:" + cacheKey(normalizedPincode, latitude, longitude);
        ServiceabilityCheckResponse redisCached = readFromRedis(cacheKey);
        if (redisCached != null) {
            serviceabilityCacheHitCounter.increment();
            return redisCached;
        }
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
            serviceabilityCacheHitCounter.increment();
            return cached.response;
        }
        serviceabilityCacheMissCounter.increment();

        GeoFenceValidationService.GeoResolution resolution = geoFenceValidationService.resolveServiceability(
                normalizedPincode,
                latitude,
                longitude,
                null
        );
        ServiceabilityCheckResponse response = toResponse(resolution);
        response = applyDynamicCapacitySignal(response, latitude, longitude);
        if (!response.isServiceable()) {
            serviceabilityFailureCounter.increment();
            unsupportedAreaCounter.increment();
            Map<String, Object> payload = new HashMap<>();
            payload.put("serviceable", false);
            payload.put("reason", response.getReason());
            payload.put("pincode", normalizedPincode);
            geoEventPublisher.publish(GeoEventType.SERVICEABILITY_CHANGED, null, payload);
        }
        cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + cacheTtlMs));
        writeToRedis(cacheKey, response);
        return response;
    }

    @Override
    public ServiceabilityCheckResponse resolveForCustomer(Long customerId) {
        if (customerId == null) {
            ServiceabilityCheckResponse response = new ServiceabilityCheckResponse();
            response.setServiceable(false);
            response.setReason("MISSING_CUSTOMER_CONTEXT");
            return response;
        }
        CustomerCurrentLocation location = customerCurrentLocationRepository.findByCustomerId(customerId).orElse(null);
        if (location == null) {
            ServiceabilityCheckResponse response = new ServiceabilityCheckResponse();
            response.setServiceable(false);
            response.setReason("CURRENT_LOCATION_NOT_FOUND");
            return response;
        }

        GeoFenceValidationService.GeoResolution resolution = geoFenceValidationService.resolveServiceability(
                location.getPincode(),
                location.getLatitude() == null ? null : location.getLatitude().doubleValue(),
                location.getLongitude() == null ? null : location.getLongitude().doubleValue(),
                location.getCity()
        );
        return toResponse(resolution);
    }

    private ServiceabilityCheckResponse applyDynamicCapacitySignal(ServiceabilityCheckResponse response, Double latitude, Double longitude) {
        if (!dynamicCapacityEnabled) {
            return response;
        }
        if (!response.isServiceable() || latitude == null || longitude == null) {
            return response;
        }
        if (isTemporarilyRestricted(latitude, longitude)) {
            response.setServiceable(false);
            response.setReason("AREA_TEMPORARILY_UNAVAILABLE");
            return response;
        }
        List<Long> nearbyPartnerIds = currentPartnerLocationRepository.findNearbyPartnerIds(
                latitude,
                longitude,
                dynamicPartnerRadiusKm,
                200
        );
        if (nearbyPartnerIds.isEmpty()) {
            response.setServiceable(false);
            response.setReason("NO_NEARBY_PARTNER_AVAILABLE");
            return response;
        }
        List<PartnerShiftEntity> shifts = partnerShiftRepository.findByPartnerIdInAndDeletedFalse(nearbyPartnerIds);
        int activeCapacity = 0;
        for (PartnerShiftEntity shift : shifts) {
            if (shift.isOnline() && shift.getActiveJobsCount() < shift.getMaxActiveJobs()) {
                activeCapacity++;
            }
        }
        int requiredCapacity = isPeakHour() ? dynamicMinActivePartners + 1 : dynamicMinActivePartners;
        if (activeCapacity < requiredCapacity) {
            response.setServiceable(false);
            response.setReason("PEAK_HOUR_OVERLOAD");
        }
        return response;
    }

    private boolean isPeakHour() {
        int hour = LocalTime.now().getHour();
        if (peakHourStart <= peakHourEnd) {
            return hour >= peakHourStart && hour < peakHourEnd;
        }
        return hour >= peakHourStart || hour < peakHourEnd;
    }

    private boolean isTemporarilyRestricted(double lat, double lng) {
        return restrictedAreaOpsService.isRestricted(lat, lng);
    }

    private ServiceabilityCheckResponse toResponse(GeoFenceValidationService.GeoResolution resolution) {
        ServiceabilityCheckResponse response = new ServiceabilityCheckResponse();
        response.setServiceable(resolution.isServiceable());
        response.setReason(resolution.getReason());
        response.setMatchedCityId(resolution.getMatchedCityId());
        response.setMatchedZoneId(resolution.getMatchedZoneId());
        response.setMatchedPincode(resolution.getMatchedPincode());
        if (resolution.getMatchedZoneId() != null) {
            response.setServiceZones(List.of("zone:" + resolution.getMatchedZoneId()));
        } else if (resolution.getMatchedPincode() != null) {
            response.setServiceZones(List.of("pincode:" + resolution.getMatchedPincode()));
        } else {
            response.setServiceZones(List.of());
        }
        response.setSupportedServices(response.isServiceable() ? List.of("STANDARD") : List.of());
        return response;
    }

    private String cacheKey(String pincode, Double latitude, Double longitude) {
        long lat = latitude == null ? 0L : Math.round(latitude * 100000.0d);
        long lng = longitude == null ? 0L : Math.round(longitude * 100000.0d);
        return pincode + ":" + lat + ":" + lng;
    }

    private String normalizePincode(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 6 ? digits : "";
    }

    private ServiceabilityCheckResponse readFromRedis(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, ServiceabilityCheckResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeToRedis(String key, ServiceabilityCheckResponse response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), Duration.ofMillis(cacheTtlMs));
        } catch (Exception ignored) {
            // fallback to local cache only
        }
    }

    private record CacheEntry(ServiceabilityCheckResponse response, long expiresAtMs) {
    }
}
