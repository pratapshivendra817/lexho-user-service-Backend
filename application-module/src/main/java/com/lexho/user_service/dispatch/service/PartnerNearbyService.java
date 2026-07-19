package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.dispatch.dto.PartnerNearbyRequest;
import com.lexho.user_service.dispatch.dto.PartnerNearbyResponse;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PartnerNearbyService {
    private final PartnerGeoIndexService partnerGeoIndexService;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final long staleLocationMinutes;
    private final Timer nearbyLatencyTimer;
    private final Counter nearbyEmptyCounter;
    private final Counter nearbyMatchCounter;

    public PartnerNearbyService(PartnerGeoIndexService partnerGeoIndexService,
                                PartnerShiftRepository partnerShiftRepository,
                                PartnerProfileRepository partnerProfileRepository,
                                CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                MeterRegistry meterRegistry,
                                @Value("${app.dispatch.partner-location-stale-minutes:10}") long staleLocationMinutes) {
        this.partnerGeoIndexService = partnerGeoIndexService;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.staleLocationMinutes = Math.max(2L, Math.min(60L, staleLocationMinutes));
        this.nearbyLatencyTimer = Timer.builder("dispatch.partner_nearby.latency").register(meterRegistry);
        this.nearbyEmptyCounter = Counter.builder("dispatch.partner_nearby.empty").register(meterRegistry);
        this.nearbyMatchCounter = Counter.builder("dispatch.partner_nearby.matches").register(meterRegistry);
    }

    public PartnerNearbyResponse findNearby(PartnerNearbyRequest request) {
        return nearbyLatencyTimer.record(() -> findNearbyInternal(request));
    }

    private PartnerNearbyResponse findNearbyInternal(PartnerNearbyRequest request) {
        Map<Long, Double> distanceByPartner = new HashMap<>();
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> geoMatches = partnerGeoIndexService.findNearby(
            request.lat,
            request.lng,
            request.radiusKm,
            300
        );
        if (geoMatches.isEmpty()) {
            List<Long> dbCandidates = currentPartnerLocationRepository.findNearbyPartnerIds(
                    request.lat,
                    request.lng,
                    request.radiusKm,
                    300
            );
            for (Long id : dbCandidates) {
                candidateIdsFromDbFallback(id, distanceByPartner, request.lat, request.lng);
            }
        }
        if (geoMatches.isEmpty() && distanceByPartner.isEmpty()) {
            nearbyEmptyCounter.increment();
            return empty();
        }

        Set<Long> candidateIds = new HashSet<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : geoMatches) {
            Long partnerId = parsePartnerId(result.getContent().getName());
            if (partnerId == null) {
                continue;
            }
            candidateIds.add(partnerId);
            if (result.getDistance() != null) {
                distanceByPartner.put(partnerId, result.getDistance().getValue());
            }
        }
        if (candidateIds.isEmpty()) {
            candidateIds.addAll(distanceByPartner.keySet());
        }
        if (candidateIds.isEmpty()) {
            nearbyEmptyCounter.increment();
            return empty();
        }

        Map<Long, PartnerShiftEntity> shifts = new HashMap<>();
        for (PartnerShiftEntity shift : partnerShiftRepository.findByPartnerIdInAndDeletedFalse(candidateIds)) {
            shifts.put(shift.getPartnerId(), shift);
        }

        Map<Long, PartnerProfile> profiles = new HashMap<>();
        for (PartnerProfile profile : partnerProfileRepository.findAllById(candidateIds)) {
            profiles.put(profile.getId(), profile);
        }

        Map<Long, CurrentPartnerLocationEntity> locations = new HashMap<>();
        for (CurrentPartnerLocationEntity location : currentPartnerLocationRepository.findByPartnerIdInAndDeletedFalse(candidateIds)) {
            locations.put(location.getPartnerId(), location);
        }

        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(staleLocationMinutes);
        String requestedService = normalizeServiceType(request.serviceType);
        List<PartnerNearbyResponse.PartnerSummary> partners = new ArrayList<>();

        for (Long partnerId : candidateIds) {
            PartnerShiftEntity shift = shifts.get(partnerId);
            PartnerProfile profile = profiles.get(partnerId);
            CurrentPartnerLocationEntity location = locations.get(partnerId);
            if (shift == null || profile == null || location == null) {
                continue;
            }
            if (!shift.isOnline() || !profile.isOnline() || profile.isDeleted()) {
                continue;
            }
            if (shift.getActiveJobsCount() >= shift.getMaxActiveJobs()) {
                continue;
            }
            if (location.getUpdatedAt() == null || location.getUpdatedAt().isBefore(staleCutoff)) {
                partnerGeoIndexService.removePartner(partnerId);
                continue;
            }
            if (!requestedService.isEmpty() && !matchesService(profile, requestedService)) {
                continue;
            }

            PartnerNearbyResponse.PartnerSummary summary = new PartnerNearbyResponse.PartnerSummary();
            summary.partnerId = partnerId;
            summary.distanceKm = round3(distanceByPartner.getOrDefault(partnerId, 9999.0d));
            summary.rating = profile.getRating() == null ? 0.0d : profile.getRating();
            summary.activeJobs = shift.getActiveJobsCount();
            summary.maxActiveJobs = shift.getMaxActiveJobs();
            summary.online = true;
            summary.serviceCategory = profile.getServiceCategory();
            summary.lat = location.getLatitude() == null ? null : location.getLatitude().doubleValue();
            summary.lng = location.getLongitude() == null ? null : location.getLongitude().doubleValue();
            summary.geohash = (summary.lat == null || summary.lng == null) ? null : GeoHashUtil.encode(summary.lat, summary.lng, 7);
            summary.lastSeenAt = location.getUpdatedAt().atOffset(ZoneOffset.UTC).toString();
            partners.add(summary);
        }

        partners.sort(Comparator
                .comparing((PartnerNearbyResponse.PartnerSummary p) -> p.distanceKm)
                .thenComparing((PartnerNearbyResponse.PartnerSummary p) -> p.rating, Comparator.reverseOrder())
                .thenComparing(p -> p.activeJobs));

        PartnerNearbyResponse response = new PartnerNearbyResponse();
        response.partners = partners.size() > 50 ? partners.subList(0, 50) : partners;
        if (response.partners.isEmpty()) {
            nearbyEmptyCounter.increment();
        } else {
            nearbyMatchCounter.increment(response.partners.size());
        }
        return response;
    }

    private boolean matchesService(PartnerProfile profile, String requestedService) {
        String partnerService = normalizeServiceType(profile.getServiceCategory());
        return partnerService.contains(requestedService) || requestedService.contains(partnerService);
    }

    private String normalizeServiceType(String serviceType) {
        if (serviceType == null) {
            return "";
        }
        return serviceType.trim().toLowerCase();
    }

    private PartnerNearbyResponse empty() {
        PartnerNearbyResponse response = new PartnerNearbyResponse();
        response.partners = List.of();
        return response;
    }

    private Long parsePartnerId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double round3(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 1000.0d) / 1000.0d;
    }

    private void candidateIdsFromDbFallback(Long partnerId, Map<Long, Double> distanceByPartner, double lat, double lng) {
        if (partnerId == null) {
            return;
        }
        currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId).ifPresent(location -> {
            if (location.getLatitude() != null && location.getLongitude() != null) {
                distanceByPartner.put(partnerId, haversineKm(
                        lat,
                        lng,
                        location.getLatitude().doubleValue(),
                        location.getLongitude().doubleValue()
                ));
            }
        });
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
}
