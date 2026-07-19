package com.lexho.user_service.customer.location.service.impl;

import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.entity.LocationAuditLog;
import com.lexho.user_service.customer.location.exception.AddressNotFoundException;
import com.lexho.user_service.customer.location.exception.InvalidCoordinatesException;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.repository.LocationAuditLogRepository;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ReverseGeocodePipelineService;
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LocationServiceImpl implements LocationService {
    private final CustomerCurrentLocationRepository repository;
    private final ReverseGeocodeService reverseGeocodeService;
    private final ReverseGeocodePipelineService reverseGeocodePipelineService;
    private final ServiceabilityService serviceabilityService;
    private final LocationAuditLogRepository locationAuditLogRepository;
    private final Counter spoofAttemptCounter;
    private final Counter timeoutRateCounter;
    private final GeoEventPublisher geoEventPublisher;
    private final double impossibleJumpSpeedKmh;
    private final long maxEventAgeMs;
    private final long reverseGeocodeMaxWaitMs;

    public LocationServiceImpl(CustomerCurrentLocationRepository repository,
                               ReverseGeocodeService reverseGeocodeService,
                               ReverseGeocodePipelineService reverseGeocodePipelineService,
                               ServiceabilityService serviceabilityService,
                               LocationAuditLogRepository locationAuditLogRepository,
                               GeoEventPublisher geoEventPublisher,
                               MeterRegistry meterRegistry,
                               @Value("${app.location.risk.impossible-jump-speed-kmh:300}") double impossibleJumpSpeedKmh,
                               @Value("${app.location.max-event-age-seconds:300}") long maxEventAgeSeconds,
                               @Value("${app.location.reverse-geocode.max-wait-ms:500}") long reverseGeocodeMaxWaitMs) {
        this.repository = repository;
        this.reverseGeocodeService = reverseGeocodeService;
        this.reverseGeocodePipelineService = reverseGeocodePipelineService;
        this.serviceabilityService = serviceabilityService;
        this.locationAuditLogRepository = locationAuditLogRepository;
        this.geoEventPublisher = geoEventPublisher;
        this.spoofAttemptCounter = Counter.builder("location.risk.spoof_attempts").register(meterRegistry);
        this.timeoutRateCounter = Counter.builder("location.api.timeout_rate").register(meterRegistry);
        this.impossibleJumpSpeedKmh = impossibleJumpSpeedKmh;
        this.maxEventAgeMs = Math.max(30L, maxEventAgeSeconds) * 1000L;
        this.reverseGeocodeMaxWaitMs = Math.max(100L, reverseGeocodeMaxWaitMs);
    }

    @Override
    @Transactional
    public LocationUpdateResponse updateCurrentLocation(Long customerId, LocationUpdateRequest request) {
        validateCoordinates(request.latitude, request.longitude);
        validateFreshness(request.eventTimestampMs);
        CustomerCurrentLocation existing = repository.findByCustomerId(customerId).orElseGet(CustomerCurrentLocation::new);
        String incomingFingerprint = fingerprint(request.latitude, request.longitude, request.idempotencyKey);
        if (request.idempotencyKey != null
                && request.idempotencyKey.equals(existing.getLastIdempotencyKey())
                && incomingFingerprint.equals(existing.getLastLocationFingerprint())) {
            return toResponse(existing);
        }

        boolean isAutoGps = request.detectionSource != null && "GPS_AUTO".equalsIgnoreCase(request.detectionSource);
        boolean preserveManualAddress = existing.isManualConfirmed()
                && isAutoGps
                && isWithinMeters(existing, request.latitude, request.longitude, 50.0d);

        ReverseGeocodeResponse reverse = preserveManualAddress
                ? new ReverseGeocodeResponse()
                : reverseWithBoundedWait(request.latitude, request.longitude);
        String pincode = preserveManualAddress
                ? normalizePincode(existing.getPincode())
                : ReverseGeocodingService.normalizePincode(reverse.pincode);
        ServiceabilityCheckResponse serviceability = serviceabilityService.resolve(pincode, request.latitude, request.longitude);
        if (serviceability == null) {
            serviceability = new ServiceabilityCheckResponse();
        }
        if (!preserveManualAddress && hasText(serviceability.getMatchedPincode())) {
            pincode = normalizePincode(serviceability.getMatchedPincode());
        }
        boolean serviceable = serviceability.isServiceable();

        CustomerCurrentLocation location = existing;
        location.setCustomerId(customerId);
        location.setLatitude(BigDecimal.valueOf(request.latitude));
        location.setLongitude(BigDecimal.valueOf(request.longitude));
        if (!preserveManualAddress) {
            location.setFormattedAddress(ReverseGeocodingService.normalizeAddressText(reverse.formattedAddress));
            location.setCity(ReverseGeocodingService.normalizeLocality(reverse.city));
            location.setState(ReverseGeocodingService.normalizeAddressText(reverse.state));
            location.setPincode(pincode);
        }
        location.setServiceAvailable(serviceable);
        location.setAccuracyMeters(scale(request.accuracyMeters, 2));
        location.setConfidenceScore(scale(request.confidenceScore, 4));
        location.setLocationAgeSeconds(request.locationAgeSeconds);
        location.setDetectionSource(request.detectionSource == null ? null : request.detectionSource.trim());
        location.setMockLocation(Boolean.TRUE.equals(request.mockLocation));
        boolean suspiciousJump = isSuspiciousJump(existing, request.latitude, request.longitude);
        location.setSuspiciousJump(suspiciousJump);
        int spoofCount = location.getSpoofAttemptCount() == null ? 0 : location.getSpoofAttemptCount();
        if (Boolean.TRUE.equals(request.mockLocation)) {
            spoofCount += 1;
            spoofAttemptCounter.increment();
        }
        location.setSpoofAttemptCount(spoofCount);
        double riskScore = computeRiskScore(Boolean.TRUE.equals(request.mockLocation), suspiciousJump, spoofCount, request.confidenceScore);
        location.setRiskScore(BigDecimal.valueOf(riskScore));
        if (Boolean.TRUE.equals(request.manualConfirmed)) {
            location.setManualConfirmed(true);
        }
        location.setLastIdempotencyKey(request.idempotencyKey == null ? null : request.idempotencyKey.trim());
        location.setLastLocationFingerprint(incomingFingerprint);
        repository.save(location);
        Map<String, Object> locationPayload = new HashMap<>();
        locationPayload.put("lat", request.latitude);
        locationPayload.put("lng", request.longitude);
        locationPayload.put("pincode", pincode);
        locationPayload.put("serviceable", serviceable);
        geoEventPublisher.publish(GeoEventType.LOCATION_UPDATED, customerId, locationPayload);
        if (Boolean.TRUE.equals(request.manualConfirmed)) {
            Map<String, Object> confirmPayload = new HashMap<>();
            confirmPayload.put("pincode", pincode);
            geoEventPublisher.publish(GeoEventType.ADDRESS_CONFIRMED, customerId, confirmPayload);
        }
        if (Boolean.TRUE.equals(request.mockLocation) || suspiciousJump || riskScore >= 50.0d) {
            auditLocation(customerId, location, "RISK_SIGNAL", "mock/suspicious/high-risk update");
            geoEventPublisher.publish(GeoEventType.SPOOF_DETECTED, customerId, Map.of(
                    "mockLocation", Boolean.TRUE.equals(request.mockLocation),
                    "suspiciousJump", suspiciousJump,
                    "riskScore", riskScore
            ));
        }

        LocationUpdateResponse response = toResponse(location);
        response.formattedAddress = firstText(reverse.formattedAddress, response.formattedAddress);
        response.area = firstText(reverse.area, response.area);
        response.city = firstText(reverse.city, response.city);
        response.state = firstText(reverse.state, response.state);
        response.country = firstText(reverse.country, response.country, "India");
        response.pincode = pincode;
        response.landmark = firstText(reverse.landmark, response.landmark);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LocationUpdateResponse getCurrentLocation(Long customerId) {
        CustomerCurrentLocation location = repository.findByCustomerId(customerId)
                .orElseThrow(() -> new AddressNotFoundException("Current location not found"));
        return toResponse(location);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            timeoutRateCounter.increment();
            throw new InvalidCoordinatesException("Invalid latitude/longitude");
        }
    }

    private void validateFreshness(Long eventTimestampMs) {
        if (eventTimestampMs == null) {
            return;
        }
        long ageMs = Duration.between(Instant.ofEpochMilli(eventTimestampMs), Instant.now()).toMillis();
        if (ageMs > maxEventAgeMs) {
            throw new InvalidCoordinatesException("Stale location event");
        }
    }

    private ReverseGeocodeResponse reverseWithBoundedWait(double latitude, double longitude) {
        try {
            CompletableFuture<ReverseGeocodeResponse> future = reverseGeocodePipelineService.reverseAsync(latitude, longitude);
            return future.completeOnTimeout(new ReverseGeocodeResponse(), reverseGeocodeMaxWaitMs, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> new ReverseGeocodeResponse())
                    .join();
        } catch (Exception ignored) {
            return new ReverseGeocodeResponse();
        }
    }

    private String normalizePincode(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 6 ? digits : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocationUpdateResponse toResponse(CustomerCurrentLocation location) {
        LocationUpdateResponse response = new LocationUpdateResponse();
        response.serviceAvailable = location.isServiceAvailable();
        response.formattedAddress = location.getFormattedAddress();
        response.city = location.getCity();
        response.state = location.getState();
        response.country = "India";
        response.pincode = location.getPincode();
        response.latitude = location.getLatitude() == null ? null : location.getLatitude().doubleValue();
        response.longitude = location.getLongitude() == null ? null : location.getLongitude().doubleValue();
        return response;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String fingerprint(Double lat, Double lng, String idempotencyKey) {
        String key = idempotencyKey == null ? "" : idempotencyKey.trim();
        return Math.round(lat * 100000d) + ":" + Math.round(lng * 100000d) + ":" + key;
    }

    private BigDecimal scale(Double value, int precision) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
    }

    private boolean isSuspiciousJump(CustomerCurrentLocation existing, double newLat, double newLng) {
        if (existing.getLatitude() == null || existing.getLongitude() == null) {
            return false;
        }
        double meters = haversineMeters(existing.getLatitude().doubleValue(), existing.getLongitude().doubleValue(), newLat, newLng);
        // Using a conservative 60-second minimum delta for retry-heavy mobile traffic.
        double kmh = (meters / 1000.0d) * 60.0d;
        return kmh > impossibleJumpSpeedKmh;
    }

    private boolean isWithinMeters(CustomerCurrentLocation existing, double newLat, double newLng, double thresholdMeters) {
        if (existing.getLatitude() == null || existing.getLongitude() == null) {
            return false;
        }
        double meters = haversineMeters(existing.getLatitude().doubleValue(), existing.getLongitude().doubleValue(), newLat, newLng);
        return meters <= thresholdMeters;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private double computeRiskScore(boolean mockLocation, boolean suspiciousJump, int spoofCount, Double confidenceScore) {
        double score = 0.0d;
        if (mockLocation) {
            score += 45.0d;
        }
        if (suspiciousJump) {
            score += 30.0d;
        }
        score += Math.min(25.0d, spoofCount * 5.0d);
        if (confidenceScore != null && confidenceScore < 0.5d) {
            score += 10.0d;
        }
        return Math.min(100.0d, Math.max(0.0d, score));
    }

    private void auditLocation(Long customerId, CustomerCurrentLocation location, String eventType, String reason) {
        try {
            LocationAuditLog log = new LocationAuditLog();
            log.setCustomerId(customerId);
            log.setLatitude(location.getLatitude());
            log.setLongitude(location.getLongitude());
            log.setPincode(location.getPincode());
            log.setSource(location.getDetectionSource());
            log.setMockLocation(location.isMockLocation());
            log.setSuspiciousJump(location.isSuspiciousJump());
            log.setRiskScore(location.getRiskScore() == null ? BigDecimal.ZERO : location.getRiskScore());
            log.setEventType(eventType);
            log.setEventReason(reason);
            locationAuditLogRepository.save(log);
        } catch (Exception ignored) {
            // non-blocking audit
        }
    }
}
