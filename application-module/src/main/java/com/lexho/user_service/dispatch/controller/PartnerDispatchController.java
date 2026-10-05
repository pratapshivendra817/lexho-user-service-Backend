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
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.util.GeoDistanceCalculator;
import com.lexho.booking.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PartnerDispatchController.class);
    private static final String PARTNER_CACHE_PREFIX = "PARTNER:";
    private static final String ONLINE_KEY = "online";
    private static final String USER_ID_KEY = "userId";
    private static final String PARTNER_ID_KEY = "partnerId";
    private static final String CURRENT_PINCODE_KEY = "currentPincode";
    private static final String TRACKING_STATE_KEY = "trackingState";
    private static final String ACCEPTED_KEY = "accepted";
    private static final String REASON_KEY = "reason";

    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final DeprecationControlService deprecationControlService;
    private final DeprecatedApiUsageService deprecatedApiUsageService;
    private final PartnerSkillService partnerSkillService;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
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
                                     PartnerOperationalProfileRepository partnerProfileRepository,
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
    public ApiResponse<Map<String, Object>> startShift(@RequestAttribute(USER_ID_KEY) Long partnerId,
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
            shift.setCreatedBy(PARTNER_CACHE_PREFIX + partnerId);
        }
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        geoEventPublisher.publish(GeoEventType.PARTNER_ONLINE, partnerId, Map.of(ONLINE_KEY, true));
        log.info("SHIFT_STARTED partnerId={} onlineStatus=ONLINE serviceAvailability=unchanged", partnerId);
        Map<String, Object> response = new HashMap<>();
        response.put(PARTNER_ID_KEY, partnerId);
        response.put(ONLINE_KEY, true);
        response.put(CURRENT_PINCODE_KEY, shift.getCurrentPincode());
        return ApiResponse.success("Partner shift started", response);
    }

    @PostMapping("/shift/end")
    public ApiResponse<Map<String, Object>> endShift(@RequestAttribute(USER_ID_KEY) Long partnerId,
                                                      @RequestBody(required = false) PartnerShiftEndRequest request) {
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElseGet(PartnerShiftEntity::new);
        shift.setPartnerId(partnerId);
        shift.setOnline(false);
        shift.setShiftEnd((request != null && request.shiftEnd != null) ? request.shiftEnd : LocalDateTime.now());
        if (shift.getId() == null) {
            shift.setCreatedBy(PARTNER_CACHE_PREFIX + partnerId);
        }
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        partnerGeoIndexService.removePartner(partnerId);
        geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId, Map.of(ONLINE_KEY, false));
        log.info("SHIFT_ENDED partnerId={} onlineStatus=OFFLINE activeBookingCancellation=false", partnerId);
        return ApiResponse.success("Partner shift ended", Map.of(
                PARTNER_ID_KEY, partnerId,
                ONLINE_KEY, false
        ));
    }

    @PatchMapping("/availability")
    public ApiResponse<Map<String, Object>> updateAvailability(@RequestAttribute(USER_ID_KEY) Long partnerId,
                                                                @Valid @RequestBody PartnerAvailabilityUpdateRequest request) {
        return updateAvailabilityInternal(partnerId, request);
    }

    @PutMapping("/availability")
    public ApiResponse<Map<String, Object>> updateAvailabilityPut(@RequestAttribute(USER_ID_KEY) Long partnerId,
                                                                   @Valid @RequestBody PartnerAvailabilityUpdateRequest request) {
        return updateAvailabilityInternal(partnerId, request);
    }

    @GetMapping("/shift/history")
    public ApiResponse<List<Map<String, Object>>> shiftHistory(@RequestAttribute(USER_ID_KEY) Long partnerId) {
        List<Map<String, Object>> history = partnerShiftRepository.findShiftHistoryByPartnerId(partnerId).stream()
                .map(this::toShiftHistoryItem)
                .toList();
        return ApiResponse.success("Partner shift history fetched", history);
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(@RequestAttribute(USER_ID_KEY) Long partnerId) {
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
                geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId, Map.of(ONLINE_KEY, false));
                log.info("OFFLINE partnerId={} newDispatch=false activeBookingCancellation=false", partnerId);
            } else {
                geoEventPublisher.publish(GeoEventType.PARTNER_ONLINE, partnerId, Map.of(ONLINE_KEY, true));
                log.info("ONLINE partnerId={}", partnerId);
            }
        }
        if (request.maxActiveJobs != null) {
            shift.setMaxActiveJobs(request.maxActiveJobs);
        }
        if (shift.getId() == null) {
            shift.setCreatedBy(PARTNER_CACHE_PREFIX + partnerId);
        }
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        Map<String, Object> response = new HashMap<>();
        response.put(PARTNER_ID_KEY, partnerId);
        response.put(ONLINE_KEY, shift.isOnline());
        response.put(CURRENT_PINCODE_KEY, shift.getCurrentPincode());
        response.put("maxActiveJobs", shift.getMaxActiveJobs());
        return ApiResponse.success("Partner availability updated", response);
    }

    private Map<String, Object> toShiftHistoryItem(PartnerShiftEntity shift) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", shift.getId());
        item.put(PARTNER_ID_KEY, shift.getPartnerId());
        item.put(ONLINE_KEY, shift.isOnline());
        item.put("shiftStart", shift.getShiftStart());
        item.put("shiftEnd", shift.getShiftEnd());
        item.put(CURRENT_PINCODE_KEY, shift.getCurrentPincode());
        item.put("activeJobsCount", shift.getActiveJobsCount());
        item.put("maxActiveJobs", shift.getMaxActiveJobs());
        item.put("updatedAt", shift.getUpdatedAt());
        return item;
    }

    @Deprecated
    @Hidden
    @PostMapping("/shift")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateShift(@RequestAttribute(USER_ID_KEY) Long partnerId,
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
            shift.setCreatedBy(PARTNER_CACHE_PREFIX + partnerId);
        }
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        Map<String, Object> response = new HashMap<>();
        response.put(PARTNER_ID_KEY, partnerId);
        response.put(ONLINE_KEY, shift.isOnline());
        response.put(CURRENT_PINCODE_KEY, shift.getCurrentPincode());
        return ResponseEntity.ok()
                .headers(deprecationHeaders("/api/partner/dispatch/shift/start, /api/partner/dispatch/shift/end, /api/partner/dispatch/availability"))
                .body(ApiResponse.success("Partner shift updated (deprecated route)", response));
    }

    @PostMapping("/location")
    public ApiResponse<Map<String, Object>> updateLocation(@RequestAttribute(USER_ID_KEY) Long partnerId,
                                                            @Valid @RequestBody PartnerLocationUpdateRequest request) {
        geoIngestCounter.increment();
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        if (shift == null || !shift.isOnline()) {
            throw new BadRequestException("Partner must be online before updating location");
        }
        if (Boolean.FALSE.equals(request.gpsEnabled)) {
            return disableGps(partnerId, shift);
        }

        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findAnyByPartnerId(partnerId)
                .orElseGet(CurrentPartnerLocationEntity::new);
        TrackingLifecycleState requestedState = requestedTrackingState(shift, location, request);
        TrackingLifecycleState trackingState = trackingLifecycleService.resolveState(
                requestedState,
                request.latitude,
                request.longitude,
                request.customerLatitude,
                request.customerLongitude
        );
        long minIngestMs = trackingLifecycleService.ingestMinIntervalMs(trackingState);
        ApiResponse<Map<String, Object>> throttled = throttleIfNeeded(partnerId, location, trackingState, minIngestMs);
        if (throttled != null) {
            return throttled;
        }

        if (trackingLifecycleService.isTerminal(trackingState)) {
            return stopTerminalTracking(partnerId, request, shift, trackingState);
        }

        PartnerLocationOptimizationService.Decision decision =
                partnerLocationOptimizationService.evaluate(location.getId() == null ? null : location, request);
        if (!decision.significantMovement()) {
            geoFilteredCounter.increment();
        }

        updateLocationEntity(partnerId, request, location, decision);
        double fraudScore = partnerLocationOptimizationService.computeFraudScore(
                location.getId() == null ? null : location,
                request,
                decision.suspicious()
        );
        location.setFraudScore(BigDecimal.valueOf(fraudScore));
        location.setDeleted(false);
        if (location.getId() == null) {
            location.setCreatedBy(PARTNER_CACHE_PREFIX + partnerId);
        }
        location.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        boolean heartbeatMode = trackingState == TrackingLifecycleState.JOB_IN_PROGRESS || trackingState == TrackingLifecycleState.JOB_STARTED;
        boolean heartbeatOnly = heartbeatMode || !decision.significantMovement();
        currentPartnerLocationRepository.save(location);
        refreshPartnerGeoIndex(partnerId, request, heartbeatOnly);
        trackingLifecycleService.persistState(partnerId, trackingState, request.bookingId);
        publishMovementEvents(partnerId, request, location, trackingState, heartbeatOnly);
        publishFraudEvent(partnerId, decision, fraudScore);
        updateShiftPincode(partnerId, shift, location.getPincode());
        return ApiResponse.success("Partner location updated", Map.of(
                PARTNER_ID_KEY, partnerId,
                "pincode", location.getPincode(),
                "source", location.getSource(),
                TRACKING_STATE_KEY, trackingState.name(),
                ACCEPTED_KEY, decision.acceptUpdate(),
                "movementAccepted", decision.significantMovement(),
                "locationSeq", location.getLocationSeq(),
                "recommendedIngestMs", minIngestMs,
                "recommendedSseMs", trackingLifecycleService.sseIntervalMs(trackingState)
        ));
    }

    private ApiResponse<Map<String, Object>> disableGps(Long partnerId, PartnerShiftEntity shift) {
        partnerGeoIndexService.removePartner(partnerId);
        shift.setOnline(false);
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        geoEventPublisher.publish(GeoEventType.PARTNER_OFFLINE, partnerId,
                Map.of(ONLINE_KEY, false, REASON_KEY, "GPS_DISABLED"));
        log.info("OFFLINE partnerId={} reason=GPS_DISABLED newDispatch=false activeBookingCancellation=false", partnerId);
        return ApiResponse.success("Partner marked offline because GPS is disabled", Map.of(
                PARTNER_ID_KEY, partnerId,
                ONLINE_KEY, false,
                REASON_KEY, "GPS_DISABLED"
        ));
    }

    private TrackingLifecycleState requestedTrackingState(PartnerShiftEntity shift,
                                                          CurrentPartnerLocationEntity location,
                                                          PartnerLocationUpdateRequest request) {
        TrackingLifecycleState requestedState = TrackingLifecycleState.from(
                request.trackingState,
                resolveTrackingState(shift, location, request.latitude, request.longitude)
        );
        if (request.bookingId == null) {
            return requestedState;
        }
        Order order = orderRepository.findById(request.bookingId).orElse(null);
        return order == null
                ? requestedState
                : trackingLifecycleService.deriveFromOrder(order.getBookingType(), order.getStatus(), requestedState);
    }

    private ApiResponse<Map<String, Object>> throttleIfNeeded(Long partnerId,
                                                              CurrentPartnerLocationEntity location,
                                                              TrackingLifecycleState trackingState,
                                                              long minIngestMs) {
        if (location.getUpdatedAt() == null) {
            return null;
        }
        long elapsedMs = Duration.between(location.getUpdatedAt(), LocalDateTime.now()).toMillis();
        if (elapsedMs >= minIngestMs) {
            return null;
        }
        location.setLastHeartbeatAt(LocalDateTime.now());
        if (location.getId() != null) {
            currentPartnerLocationRepository.save(location);
        }
        return ApiResponse.success("Partner location throttled", Map.of(
                PARTNER_ID_KEY, partnerId,
                TRACKING_STATE_KEY, trackingState.name(),
                ACCEPTED_KEY, false,
                "nextUpdateAfterMs", minIngestMs - Math.max(0L, elapsedMs)
        ));
    }

    private ApiResponse<Map<String, Object>> stopTerminalTracking(Long partnerId,
                                                                  PartnerLocationUpdateRequest request,
                                                                  PartnerShiftEntity shift,
                                                                  TrackingLifecycleState trackingState) {
        trackingLifecycleService.persistState(partnerId, trackingState, request.bookingId);
        partnerGeoIndexService.removePartner(partnerId);
        shift.setOnline(false);
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
        return ApiResponse.success("Tracking stopped for terminal state", Map.of(
                PARTNER_ID_KEY, partnerId,
                TRACKING_STATE_KEY, trackingState.name(),
                ACCEPTED_KEY, false,
                "shutdown", true
        ));
    }

    private void updateLocationEntity(Long partnerId,
                                      PartnerLocationUpdateRequest request,
                                      CurrentPartnerLocationEntity location,
                                      PartnerLocationOptimizationService.Decision decision) {
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
    }

    private void refreshPartnerGeoIndex(Long partnerId, PartnerLocationUpdateRequest request, boolean heartbeatOnly) {
        if (!heartbeatOnly) {
            geoBatchingService.queueGeoPointRefresh(partnerId);
        }
        partnerGeoIndexService.upsertPartnerLocation(partnerId, request.latitude, request.longitude, heartbeatOnly);
    }

    private void publishMovementEvents(Long partnerId,
                                       PartnerLocationUpdateRequest request,
                                       CurrentPartnerLocationEntity location,
                                       TrackingLifecycleState trackingState,
                                       boolean heartbeatOnly) {
        if (heartbeatOnly) {
            return;
        }
        Map<String, Object> movedPayload = new HashMap<>();
        movedPayload.put("lat", request.latitude);
        movedPayload.put("lng", request.longitude);
        movedPayload.put("pincode", location.getPincode());
        movedPayload.put(TRACKING_STATE_KEY, trackingState.name());
        movedPayload.put("seq", location.getLocationSeq());
        if (isImmediateLifecycle(trackingState)) {
            geoBatchingService.publishImmediate(GeoEventType.PARTNER_MOVED, partnerId, movedPayload);
        } else {
            geoBatchingService.queuePartnerMoved(partnerId, movedPayload);
        }
    }

    private boolean isImmediateLifecycle(TrackingLifecycleState trackingState) {
        return trackingState == TrackingLifecycleState.ARRIVED
                || trackingState == TrackingLifecycleState.JOB_STARTED
                || trackingState == TrackingLifecycleState.JOB_COMPLETED
                || trackingState == TrackingLifecycleState.CANCELLED;
    }

    private void publishFraudEvent(Long partnerId,
                                   PartnerLocationOptimizationService.Decision decision,
                                   double fraudScore) {
        if (!decision.suspicious()) {
            return;
        }
        geoEventPublisher.publish(GeoEventType.SPOOF_DETECTED, partnerId, Map.of(
                REASON_KEY, decision.reason(),
                "score", fraudScore
        ));
    }

    private void updateShiftPincode(Long partnerId, PartnerShiftEntity shift, String pincode) {
        shift.setCurrentPincode(pincode);
        shift.setUpdatedBy(PARTNER_CACHE_PREFIX + partnerId);
        partnerShiftRepository.save(shift);
    }

    private TrackingLifecycleState resolveTrackingState(PartnerShiftEntity shift, CurrentPartnerLocationEntity existing, double newLat, double newLng) {
        if (shift.getActiveJobsCount() > 0) {
            return TrackingLifecycleState.EN_ROUTE;
        }
        if (existing.getLatitude() == null || existing.getLongitude() == null) {
            return TrackingLifecycleState.ASSIGNED;
        }
        double movedMeters = GeoDistanceCalculator.haversineMeters(
                existing.getLatitude().doubleValue(),
                existing.getLongitude().doubleValue(),
                newLat,
                newLng
        );
        return movedMeters >= movingThresholdMeters ? TrackingLifecycleState.EN_ROUTE : TrackingLifecycleState.ASSIGNED;
    }

    private HttpHeaders deprecationHeaders(String replacementPath) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Deprecated", "true");
        headers.add("X-API-Replacement", replacementPath);
        return headers;
    }

    @PutMapping("/skills")
    public ApiResponse<Map<String, Object>> upsertSkills(@RequestAttribute(USER_ID_KEY) Long partnerId,
                                                          @Valid @RequestBody PartnerSkillsUpdateRequest request) {
        return ApiResponse.success("Partner skills updated", partnerSkillService.upsertSkills(partnerId, request));
    }

    @GetMapping("/skills")
    public ApiResponse<Map<String, Object>> getSkills(@RequestAttribute(USER_ID_KEY) Long partnerId) {
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
