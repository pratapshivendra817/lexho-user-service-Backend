package com.lexho.user_service.dispatch.controller;

import com.lexho.booking.entity.Order;
import com.lexho.user_service.dispatch.dto.PartnerLocationUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerAvailabilityUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftEndRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftStartRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerGeoIndexService;
import com.lexho.user_service.dispatch.service.GeoHashUtil;
import com.lexho.user_service.dispatch.service.GeoBatchingService;
import com.lexho.user_service.dispatch.service.PartnerLocationOptimizationService;
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleState;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import com.lexho.user_service.service.DeprecationControlService;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.booking.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/partner/dispatch", "/api/v1/partner/dispatch"})
@PreAuthorize("hasRole('PARTNER')")
public class PartnerDispatchController {

    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final DeprecationControlService deprecationControlService;
    private final DeprecatedApiUsageService deprecatedApiUsageService;
    private final PartnerSkillService partnerSkillService;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerGeoIndexService partnerGeoIndexService;
    private final GeoEventPublisher geoEventPublisher;
    private final TrackingLifecycleService trackingLifecycleService;
    private final OrderRepository orderRepository;
    private final PartnerLocationOptimizationService partnerLocationOptimizationService;
    private final GeoBatchingService geoBatchingService;
    private final Counter geoIngestCounter;
    private final Counter geoFilteredCounter;
    private final Counter fraudDetectedCounter;
    private final double movingThresholdMeters;
    private final long locationUpdateIntervalSeconds;
    private final long backgroundIntervalSeconds;

    public PartnerDispatchController(PartnerShiftRepository partnerShiftRepository,
                                     CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                     ReverseGeocodingService reverseGeocodingService,
                                     DeprecationControlService deprecationControlService,
                                     DeprecatedApiUsageService deprecatedApiUsageService,
                                     PartnerSkillService partnerSkillService,
                                     PartnerProfileRepository partnerProfileRepository,
                                     PartnerGeoIndexService partnerGeoIndexService,
                                     GeoEventPublisher geoEventPublisher,
                                      TrackingLifecycleService trackingLifecycleService,
                                      OrderRepository orderRepository,
                                     PartnerLocationOptimizationService partnerLocationOptimizationService,
                                     GeoBatchingService geoBatchingService,
                                     MeterRegistry meterRegistry,
                                     @Value("${app.tracking.movement-threshold-meters:12}") double movingThresholdMeters,
                                     @Value("${app.partner.dispatch.location-update-interval-seconds:15}") long locationUpdateIntervalSeconds,
                                     @Value("${app.partner.dispatch.background-interval-seconds:30}") long backgroundIntervalSeconds) {
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.reverseGeocodingService = reverseGeocodingService;
        this.deprecationControlService = deprecationControlService;
        this.deprecatedApiUsageService = deprecatedApiUsageService;
        this.partnerSkillService = partnerSkillService;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerGeoIndexService = partnerGeoIndexService;
        this.geoEventPublisher = geoEventPublisher;
        this.trackingLifecycleService = trackingLifecycleService;
        this.orderRepository = orderRepository;
        this.partnerLocationOptimizationService = partnerLocationOptimizationService;
        this.geoBatchingService = geoBatchingService;
        this.geoIngestCounter = Counter.builder("dispatch.geo.ingest.rate").register(meterRegistry);
        this.geoFilteredCounter = Counter.builder("dispatch.geo.filtered_updates").register(meterRegistry);
        this.fraudDetectedCounter = Counter.builder("dispatch.geo.fraud_detected").register(meterRegistry);
        this.movingThresholdMeters = Math.max(3.0d, Math.min(50.0d, movingThresholdMeters));
        this.locationUpdateIntervalSeconds = locationUpdateIntervalSeconds;
        this.backgroundIntervalSeconds = backgroundIntervalSeconds;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.success("Partner dispatch config fetched", Map.of(
                "locationUpdateIntervalSeconds", locationUpdateIntervalSeconds,
                "backgroundIntervalSeconds", backgroundIntervalSeconds
        ));
    }

    @PostMapping("/shift/start")
    public ApiResponse<Map<String, Object>> startShift(@RequestAttribute("userId") Long partnerId,
                                                        @RequestBody(required = false) PartnerShiftStartRequest request) {
        ensureAvailabilityUnlocked(partnerId);
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElseGet(PartnerShiftEntity::new);
        shift.setPartnerId(partnerId);
        shift.setOnline(true);
        LocalDateTime resolvedStart = request == null ? null : request.resolveShiftStart();
        LocalDateTime resolvedEnd = request == null ? null : request.resolveShiftEnd();
        LocalDateTime now = LocalDateTime.now();
        shift.setShiftStart(resolvedStart == null ? now : resolvedStart);
        shift.setShiftEnd(resolvedEnd);
        CurrentPartnerLocationEntity existingLocation = currentPartnerLocationRepository.findByPartnerId(partnerId).orElse(null);
        shift.setCurrentPincode(existingLocation == null ? null : existingLocation.getPincode());
        if (shift.getId() == null) {
            shift.setCreatedBy("PARTNER:" + partnerId);
        }
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        geoEventPublisher.publish(GeoEventType.PARTNER_ONLINE, partnerId, Map.of("online", true));
        Map<String, Object> response = new HashMap<>();
        response.put("partnerId", partnerId);
        response.put("online", true);
        response.put("currentPincode", shift.getCurrentPincode());
        return ApiResponse.success("Partner shift started", response);
    }

    @PostMapping("/shift/end")
    public ApiResponse<Map<String, Object>> endShift(@RequestAttribute("userId") Long partnerId,
                                                      @RequestBody(required = false) PartnerShiftEndRequest request) {
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElseGet(PartnerShiftEntity::new);
        shift.setPartnerId(partnerId);
        shift.setOnline(false);
        shift.setShiftEnd((request != null && request.shiftEnd != null) ? request.shiftEnd : LocalDateTime.now());
        if (shift.getId() == null) {
            shift.setCreatedBy("PARTNER:" + partnerId);
        }
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        partnerGeoIndexService.removePartner(partnerId);
        geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId, Map.of("online", false));
        return ApiResponse.success("Partner shift ended", Map.of(
                "partnerId", partnerId,
                "online", false
        ));
    }

    @PatchMapping("/availability")
    public ApiResponse<Map<String, Object>> updateAvailability(@RequestAttribute("userId") Long partnerId,
                                                                @Valid @RequestBody PartnerAvailabilityUpdateRequest request) {
        return updateAvailabilityInternal(partnerId, request);
    }

    @PutMapping("/availability")
    public ApiResponse<Map<String, Object>> updateAvailabilityPut(@RequestAttribute("userId") Long partnerId,
                                                                   @Valid @RequestBody PartnerAvailabilityUpdateRequest request) {
        return updateAvailabilityInternal(partnerId, request);
    }

    @GetMapping("/shift/history")
    public ApiResponse<List<Map<String, Object>>> shiftHistory(@RequestAttribute("userId") Long partnerId) {
        List<Map<String, Object>> history = partnerShiftRepository.findShiftHistoryByPartnerId(partnerId).stream()
                .map(this::toShiftHistoryItem)
                .toList();
        return ApiResponse.success("Partner shift history fetched", history);
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(@RequestAttribute("userId") Long partnerId) {
        return shiftHistory(partnerId);
    }

    private ApiResponse<Map<String, Object>> updateAvailabilityInternal(Long partnerId,
                                                                        PartnerAvailabilityUpdateRequest request) {
        ensureAvailabilityUnlocked(partnerId);
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElseGet(PartnerShiftEntity::new);
        shift.setPartnerId(partnerId);
        Boolean resolvedOnline = request.resolveOnline();
        if (resolvedOnline != null) {
            shift.setOnline(resolvedOnline);
            if (!resolvedOnline) {
                partnerGeoIndexService.removePartner(partnerId);
                geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId, Map.of("online", false));
            } else {
                geoEventPublisher.publish(GeoEventType.PARTNER_ONLINE, partnerId, Map.of("online", true));
            }
        }
        if (request.maxActiveJobs != null) {
            shift.setMaxActiveJobs(request.maxActiveJobs);
        }
        if (shift.getId() == null) {
            shift.setCreatedBy("PARTNER:" + partnerId);
        }
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        Map<String, Object> response = new HashMap<>();
        response.put("partnerId", partnerId);
        response.put("online", shift.isOnline());
        response.put("currentPincode", shift.getCurrentPincode());
        response.put("maxActiveJobs", shift.getMaxActiveJobs());
        return ApiResponse.success("Partner availability updated", response);
    }

    private Map<String, Object> toShiftHistoryItem(PartnerShiftEntity shift) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", shift.getId());
        item.put("partnerId", shift.getPartnerId());
        item.put("online", shift.isOnline());
        item.put("shiftStart", shift.getShiftStart());
        item.put("shiftEnd", shift.getShiftEnd());
        item.put("currentPincode", shift.getCurrentPincode());
        item.put("activeJobsCount", shift.getActiveJobsCount());
        item.put("maxActiveJobs", shift.getMaxActiveJobs());
        item.put("updatedAt", shift.getUpdatedAt());
        return item;
    }

    @Deprecated
    @Hidden
    @PostMapping("/shift")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateShift(@RequestAttribute("userId") Long partnerId,
                                                                         @Valid @RequestBody PartnerShiftUpdateRequest request) {
        deprecationControlService.ensureLegacyPartnerShiftEnabled();
        deprecatedApiUsageService.record("partner.dispatch.shift.legacy");
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElseGet(PartnerShiftEntity::new);
        shift.setPartnerId(partnerId);
        shift.setOnline(Boolean.TRUE.equals(request.isOnline));
        CurrentPartnerLocationEntity existingLocation = currentPartnerLocationRepository.findByPartnerId(partnerId).orElse(null);
        shift.setCurrentPincode(existingLocation == null ? null : existingLocation.getPincode());
        shift.setShiftStart(request.shiftStart);
        shift.setShiftEnd(request.shiftEnd);
        if (request.maxActiveJobs != null) {
            shift.setMaxActiveJobs(request.maxActiveJobs);
        }
        if (shift.getId() == null) {
            shift.setCreatedBy("PARTNER:" + partnerId);
        }
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        Map<String, Object> response = new HashMap<>();
        response.put("partnerId", partnerId);
        response.put("online", shift.isOnline());
        response.put("currentPincode", shift.getCurrentPincode());
        return ResponseEntity.ok()
                .headers(deprecationHeaders("/api/partner/dispatch/shift/start, /api/partner/dispatch/shift/end, /api/partner/dispatch/availability"))
                .body(ApiResponse.success("Partner shift updated (deprecated route)", response));
    }

    @PostMapping("/location")
    public ApiResponse<Map<String, Object>> updateLocation(@RequestAttribute("userId") Long partnerId,
                                                            @Valid @RequestBody PartnerLocationUpdateRequest request) {
        geoIngestCounter.increment();
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        if (shift == null || !shift.isOnline()) {
            throw new BadRequestException("Partner must be online before updating location");
        }
        if (Boolean.FALSE.equals(request.gpsEnabled)) {
            partnerGeoIndexService.removePartner(partnerId);
            shift.setOnline(false);
            shift.setUpdatedBy("PARTNER:" + partnerId);
            partnerShiftRepository.save(shift);
            geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId, Map.of("online", false, "reason", "GPS_DISABLED"));
            return ApiResponse.success("Partner marked offline because GPS is disabled", Map.of(
                    "partnerId", partnerId,
                    "online", false,
                    "reason", "GPS_DISABLED"
            ));
        }

        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findAnyByPartnerId(partnerId)
                .orElseGet(CurrentPartnerLocationEntity::new);

        TrackingLifecycleState requestedState = TrackingLifecycleState.from(
                request.trackingState,
                resolveTrackingState(shift, location, request.latitude, request.longitude)
        );
        if (request.bookingId != null) {
            Order order = orderRepository.findById(request.bookingId).orElse(null);
            if (order != null) {
                requestedState = trackingLifecycleService.deriveFromOrder(order.getBookingType(), order.getStatus(), requestedState);
            }
        }
        TrackingLifecycleState trackingState = trackingLifecycleService.resolveState(
                requestedState,
                request.latitude,
                request.longitude,
                request.customerLatitude,
                request.customerLongitude
        );
        long minIngestMs = trackingLifecycleService.ingestMinIntervalMs(trackingState);
        if (location.getUpdatedAt() != null) {
            long elapsedMs = Duration.between(location.getUpdatedAt(), LocalDateTime.now()).toMillis();
            if (elapsedMs < minIngestMs) {
                location.setLastHeartbeatAt(LocalDateTime.now());
                if (location.getId() != null) {
                    currentPartnerLocationRepository.save(location);
                }
                return ApiResponse.success("Partner location throttled", Map.of(
                        "partnerId", partnerId,
                        "trackingState", trackingState.name(),
                        "accepted", false,
                        "nextUpdateAfterMs", minIngestMs - Math.max(0L, elapsedMs)
                ));
            }
        }

        if (trackingLifecycleService.isTerminal(trackingState)) {
            trackingLifecycleService.persistState(partnerId, trackingState, request.bookingId);
            partnerGeoIndexService.removePartner(partnerId);
            shift.setOnline(false);
            shift.setUpdatedBy("PARTNER:" + partnerId);
            partnerShiftRepository.save(shift);
            return ApiResponse.success("Tracking stopped for terminal state", Map.of(
                    "partnerId", partnerId,
                    "trackingState", trackingState.name(),
                    "accepted", false,
                    "shutdown", true
            ));
        }

        PartnerLocationOptimizationService.Decision decision =
                partnerLocationOptimizationService.evaluate(location.getId() == null ? null : location, request);
        if (!decision.significantMovement()) {
            geoFilteredCounter.increment();
        }

        location.setPartnerId(partnerId);
        location.setLatitude(BigDecimal.valueOf(request.latitude));
        location.setLongitude(BigDecimal.valueOf(request.longitude));
        ReverseGeocodeResponse geo = reverseGeocodingService.reverse(request.latitude, request.longitude);
        location.setPincode(geo.pincode == null || geo.pincode.isBlank() ? null : geo.pincode.trim());
        location.setSource(request.source == null || request.source.isBlank() ? "GPS" : request.source.trim());
        location.setGeohash(GeoHashUtil.encode(request.latitude, request.longitude, 7));
        location.setAccuracyMeters(request.accuracyMeters == null ? null : BigDecimal.valueOf(request.accuracyMeters));
        location.setHeadingDegrees(request.headingDegrees == null ? null : BigDecimal.valueOf(request.headingDegrees));
        location.setSpeedKmh(request.speedKmh == null ? null : BigDecimal.valueOf(request.speedKmh));
        location.setLocationSeq(location.getLocationSeq() + 1L);
        location.setLastHeartbeatAt(LocalDateTime.now());
        if (decision.suspicious()) {
            fraudDetectedCounter.increment();
            location.setFraudReason(decision.reason());
            location.setLastSuspiciousAt(LocalDateTime.now());
        }
        double fraudScore = partnerLocationOptimizationService.computeFraudScore(
                location.getId() == null ? null : location,
                request,
                decision.suspicious()
        );
        location.setFraudScore(BigDecimal.valueOf(fraudScore));
        location.setDeleted(false);
        if (location.getId() == null) {
            location.setCreatedBy("PARTNER:" + partnerId);
        }
        location.setUpdatedBy("PARTNER:" + partnerId);
        boolean heartbeatMode = trackingState == TrackingLifecycleState.JOB_IN_PROGRESS || trackingState == TrackingLifecycleState.JOB_STARTED;
        boolean heartbeatOnly = heartbeatMode || !decision.significantMovement();
        currentPartnerLocationRepository.save(location);
        if (!heartbeatOnly) {
            geoBatchingService.queueGeoPointRefresh(partnerId);
            partnerGeoIndexService.upsertPartnerLocation(partnerId, request.latitude, request.longitude, false);
        } else {
            partnerGeoIndexService.upsertPartnerLocation(partnerId, request.latitude, request.longitude, true);
        }
        trackingLifecycleService.persistState(partnerId, trackingState, request.bookingId);
        Map<String, Object> movedPayload = new HashMap<>();
        movedPayload.put("lat", request.latitude);
        movedPayload.put("lng", request.longitude);
        movedPayload.put("pincode", location.getPincode());
        movedPayload.put("trackingState", trackingState.name());
        movedPayload.put("seq", location.getLocationSeq());
        boolean immediateLifecycle = trackingState == TrackingLifecycleState.ARRIVED
                || trackingState == TrackingLifecycleState.JOB_STARTED
                || trackingState == TrackingLifecycleState.JOB_COMPLETED
                || trackingState == TrackingLifecycleState.CANCELLED;
        if (!heartbeatOnly) {
            if (immediateLifecycle) {
                geoBatchingService.publishImmediate(GeoEventType.PARTNER_MOVED, partnerId, movedPayload);
            } else {
                geoBatchingService.queuePartnerMoved(partnerId, movedPayload);
            }
        }
        if (decision.suspicious()) {
            geoEventPublisher.publish(GeoEventType.SPOOF_DETECTED, partnerId, Map.of(
                    "reason", decision.reason(),
                    "score", fraudScore
            ));
        }
        shift.setCurrentPincode(location.getPincode());
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        return ApiResponse.success("Partner location updated", Map.of(
                "partnerId", partnerId,
                "pincode", location.getPincode(),
                "source", location.getSource(),
                "trackingState", trackingState.name(),
                "accepted", decision.acceptUpdate(),
                "movementAccepted", decision.significantMovement(),
                "locationSeq", location.getLocationSeq(),
                "recommendedIngestMs", minIngestMs,
                "recommendedSseMs", trackingLifecycleService.sseIntervalMs(trackingState)
        ));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private TrackingLifecycleState resolveTrackingState(PartnerShiftEntity shift, CurrentPartnerLocationEntity existing, double newLat, double newLng) {
        if (shift.getActiveJobsCount() > 0) {
            return TrackingLifecycleState.EN_ROUTE;
        }
        if (existing.getLatitude() == null || existing.getLongitude() == null) {
            return TrackingLifecycleState.ASSIGNED;
        }
        double movedMeters = haversineKm(
                existing.getLatitude().doubleValue(),
                existing.getLongitude().doubleValue(),
                newLat,
                newLng
        ) * 1000.0d;
        return movedMeters >= movingThresholdMeters ? TrackingLifecycleState.EN_ROUTE : TrackingLifecycleState.ASSIGNED;
    }

    private HttpHeaders deprecationHeaders(String replacementPath) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Deprecated", "true");
        headers.add("X-API-Replacement", replacementPath);
        return headers;
    }

    @PutMapping("/skills")
    public ApiResponse<Map<String, Object>> upsertSkills(@RequestAttribute("userId") Long partnerId,
                                                          @Valid @RequestBody PartnerSkillsUpdateRequest request) {
        return ApiResponse.success("Partner skills updated", partnerSkillService.upsertSkills(partnerId, request));
    }

    @GetMapping("/skills")
    public ApiResponse<Map<String, Object>> getSkills(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Partner skills fetched", partnerSkillService.getSkills(partnerId));
    }

    private void ensureAvailabilityUnlocked(Long partnerId) {
        partnerProfileRepository.findById(partnerId).ifPresent(profile -> {
            if (profile.getAvailabilityLockedUntil() != null && profile.getAvailabilityLockedUntil().isAfter(LocalDateTime.now())) {
                throw new BadRequestException("Availability is locked by admin until " + profile.getAvailabilityLockedUntil());
            }
        });
    }
}
