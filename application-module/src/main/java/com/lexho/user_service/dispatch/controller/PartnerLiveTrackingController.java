package com.lexho.user_service.dispatch.controller;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.TrackingLifecycleService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/partners/live")
public class PartnerLiveTrackingController {
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final TrackingLifecycleService trackingLifecycleService;
    private final ExecutorService emitterExecutor = Executors.newCachedThreadPool();
    private final Map<Long, Snapshot> previousSnapshots = new ConcurrentHashMap<>();
    private final long idleSseMs;
    private final long movingSseMs;
    private final long activeSseMs;
    private final double movingThresholdMeters;
    private final AtomicInteger activeStreams = new AtomicInteger(0);
    private final Map<Long, Long> lastSentSeqByPartner = new ConcurrentHashMap<>();

    public PartnerLiveTrackingController(CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                         PartnerShiftRepository partnerShiftRepository,
                                         TrackingLifecycleService trackingLifecycleService,
                                         MeterRegistry meterRegistry,
                                         @Value("${app.tracking.sse.idle-ms:12000}") long idleSseMs,
                                         @Value("${app.tracking.sse.moving-ms:12000}") long movingSseMs,
                                         @Value("${app.tracking.sse.active-delivery-ms:7000}") long activeSseMs,
                                         @Value("${app.tracking.movement-threshold-meters:12}") double movingThresholdMeters) {
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.trackingLifecycleService = trackingLifecycleService;
        this.idleSseMs = Math.max(10_000L, Math.min(15_000L, idleSseMs));
        this.movingSseMs = Math.max(10_000L, Math.min(15_000L, movingSseMs));
        this.activeSseMs = Math.max(5_000L, Math.min(10_000L, activeSseMs));
        this.movingThresholdMeters = Math.max(3.0d, Math.min(50.0d, movingThresholdMeters));
        Gauge.builder("dispatch.sse.active_streams", activeStreams, AtomicInteger::get).register(meterRegistry);
    }

    @GetMapping(value = "/{partnerId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','PARTNER')")
    public SseEmitter track(@PathVariable Long partnerId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitterExecutor.submit(() -> streamPartnerLocation(partnerId, emitter));
        return emitter;
    }

    private void streamPartnerLocation(Long partnerId, SseEmitter emitter) {
        activeStreams.incrementAndGet();
        try {
            for (int i = 0; i < 40; i++) {
                Map<String, Object> payload = snapshot(partnerId);
                long seq = toLong(payload.get("seq"));
                Long lastSeq = lastSentSeqByPartner.get(partnerId);
                if (lastSeq == null || seq > lastSeq) {
                    emitter.send(SseEmitter.event().name("partner-location").data(payload));
                    lastSentSeqByPartner.put(partnerId, seq);
                } else {
                    emitter.send(SseEmitter.event().name("heartbeat").data(Map.of(
                            "partnerId", partnerId,
                            "trackingState", payload.get("trackingState"),
                            "lastSeenAt", payload.get("lastSeenAt"),
                            "seq", seq
                    )));
                }
                Thread.sleep(resolveSseIntervalMs(payload));
            }
            emitter.complete();
        } catch (IOException | InterruptedException ex) {
            emitter.completeWithError(ex);
            Thread.currentThread().interrupt();
        } finally {
            activeStreams.decrementAndGet();
        }
    }

    private Map<String, Object> snapshot(Long partnerId) {
        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId).orElse(null);
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerIdAndDeletedFalse(partnerId).orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerId", partnerId);
        payload.put("online", shift != null && shift.isOnline());
        payload.put("trackingState", resolveTrackingState(shift, location, partnerId));
        if (location != null) {
            double lat = location.getLatitude().doubleValue();
            double lng = location.getLongitude().doubleValue();
            payload.put("lat", lat);
            payload.put("lng", lng);
            payload.put("geohash", location.getGeohash());
            payload.put("pincode", location.getPincode());
            payload.put("seq", location.getLocationSeq());
            payload.put("lastSeenAt", location.getUpdatedAt() == null ? null : location.getUpdatedAt().atOffset(ZoneOffset.UTC).toString());
            Snapshot previous = previousSnapshots.get(partnerId);
            if (previous != null && location.getUpdatedAt() != null && previous.timeMs > 0) {
                long nowMs = location.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
                long deltaMs = Math.max(1L, nowMs - previous.timeMs);
                double distanceMeters = haversineMeters(previous.lat, previous.lng, lat, lng);
                double speedKmh = (distanceMeters / deltaMs) * 3_600.0d;
                payload.put("speedKmh", round2(speedKmh));
                payload.put("headingDegrees", round2(initialBearing(previous.lat, previous.lng, lat, lng)));
            } else {
                payload.put("speedKmh", 0.0d);
                payload.put("headingDegrees", 0.0d);
            }
            long currentTime = location.getUpdatedAt() == null ? 0L : location.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            previousSnapshots.put(partnerId, new Snapshot(lat, lng, currentTime));
        }
        return payload;
    }

    private long resolveSseIntervalMs(Map<String, Object> payload) {
        Object stateObj = payload.get("trackingState");
        TrackingLifecycleState state = TrackingLifecycleState.from(
                stateObj == null ? null : stateObj.toString(),
                TrackingLifecycleState.ASSIGNED
        );
        long lifecycleInterval = trackingLifecycleService.sseIntervalMs(state);
        if (state == TrackingLifecycleState.EN_ROUTE) {
            return Math.max(5_000L, Math.min(10_000L, lifecycleInterval));
        }
        if (state == TrackingLifecycleState.ARRIVED) {
            return Math.max(10_000L, Math.min(15_000L, lifecycleInterval));
        }
        if (state == TrackingLifecycleState.JOB_IN_PROGRESS || state == TrackingLifecycleState.JOB_STARTED) {
            return Math.max(5_000L, Math.min(10_000L, lifecycleInterval));
        }
        return state == TrackingLifecycleState.ASSIGNED ? movingSseMs : idleSseMs;
    }

    private String resolveTrackingState(PartnerShiftEntity shift, CurrentPartnerLocationEntity location, Long partnerId) {
        if (shift == null || !shift.isOnline()) return TrackingLifecycleState.OFFLINE.name();
        TrackingLifecycleState persisted = trackingLifecycleService.fetchState(partnerId);
        if (persisted != TrackingLifecycleState.OFFLINE) {
            return persisted.name();
        }
        if (shift.getActiveJobsCount() > 0) return TrackingLifecycleState.EN_ROUTE.name();
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) return TrackingLifecycleState.ASSIGNED.name();
        Snapshot previous = previousSnapshots.get(partnerId);
        if (previous == null) return TrackingLifecycleState.ASSIGNED.name();
        double movedMeters = haversineMeters(previous.lat, previous.lng, location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
        return movedMeters >= movingThresholdMeters ? TrackingLifecycleState.EN_ROUTE.name() : TrackingLifecycleState.ASSIGNED.name();
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private double initialBearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double lambda1 = Math.toRadians(lon1);
        double lambda2 = Math.toRadians(lon2);
        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0d) % 360.0d;
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private record Snapshot(double lat, double lng, long timeMs) {
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
