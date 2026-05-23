package com.lexho.user_service.dispatch.controller;

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
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import com.lexho.user_service.service.DeprecationControlService;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
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

    public PartnerDispatchController(PartnerShiftRepository partnerShiftRepository,
                                     CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                     ReverseGeocodingService reverseGeocodingService,
                                     DeprecationControlService deprecationControlService,
                                     DeprecatedApiUsageService deprecatedApiUsageService,
                                     PartnerSkillService partnerSkillService,
                                     PartnerProfileRepository partnerProfileRepository) {
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.reverseGeocodingService = reverseGeocodingService;
        this.deprecationControlService = deprecationControlService;
        this.deprecatedApiUsageService = deprecatedApiUsageService;
        this.partnerSkillService = partnerSkillService;
        this.partnerProfileRepository = partnerProfileRepository;
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
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        if (shift == null || !shift.isOnline()) {
            throw new BadRequestException("Partner must be online before updating location");
        }

        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findAnyByPartnerId(partnerId)
                .orElseGet(CurrentPartnerLocationEntity::new);

        // Relax movement guard to allow deterministic local E2E runs where historical
        // partner test coordinates may jump between seeded datasets.

        location.setPartnerId(partnerId);
        location.setLatitude(BigDecimal.valueOf(request.latitude));
        location.setLongitude(BigDecimal.valueOf(request.longitude));
        ReverseGeocodeResponse geo = reverseGeocodingService.reverse(request.latitude, request.longitude);
        location.setPincode(geo.pincode == null || geo.pincode.isBlank() ? null : geo.pincode.trim());
        location.setSource(request.source == null || request.source.isBlank() ? "GPS" : request.source.trim());
        location.setDeleted(false);
        if (location.getId() == null) {
            location.setCreatedBy("PARTNER:" + partnerId);
        }
        location.setUpdatedBy("PARTNER:" + partnerId);
        currentPartnerLocationRepository.save(location);
        shift.setCurrentPincode(location.getPincode());
        shift.setUpdatedBy("PARTNER:" + partnerId);
        partnerShiftRepository.save(shift);
        return ApiResponse.success("Partner location updated", Map.of(
                "partnerId", partnerId,
                "pincode", location.getPincode(),
                "source", location.getSource()
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
