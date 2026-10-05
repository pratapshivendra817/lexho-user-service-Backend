package com.lexho.user_service.dispatch.service;

import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.util.GeoDistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TrackingLifecycleService {
    private final StringRedisTemplate redisTemplate;
    private final double arrivalRadiusMeters;
    private final int geohashPrecision;
    private final long assignedIngestMs;
    private final long assignedSseMs;
    private final long enRouteIngestMs;
    private final long enRouteSseMs;
    private final long arrivedIngestMs;
    private final long arrivedSseMs;
    private final long inProgressIngestMs;
    private final long inProgressSseMs;

    public TrackingLifecycleService(StringRedisTemplate redisTemplate,
                                    @Value("${app.tracking.arrival-radius-meters:60}") double arrivalRadiusMeters,
                                    @Value("${app.tracking.arrival-geohash-precision:7}") int geohashPrecision,
                                    @Value("${app.tracking.assigned.ingest-ms:30000}") long assignedIngestMs,
                                    @Value("${app.tracking.assigned.sse-ms:20000}") long assignedSseMs,
                                    @Value("${app.tracking.en-route.ingest-ms:5000}") long enRouteIngestMs,
                                    @Value("${app.tracking.en-route.sse-ms:3000}") long enRouteSseMs,
                                    @Value("${app.tracking.arrived.ingest-ms:2000}") long arrivedIngestMs,
                                    @Value("${app.tracking.arrived.sse-ms:2000}") long arrivedSseMs,
                                    @Value("${app.tracking.in-progress.ingest-ms:30000}") long inProgressIngestMs,
                                    @Value("${app.tracking.in-progress.sse-ms:15000}") long inProgressSseMs) {
        this.redisTemplate = redisTemplate;
        this.arrivalRadiusMeters = Math.max(20.0d, Math.min(200.0d, arrivalRadiusMeters));
        this.geohashPrecision = Math.max(5, Math.min(8, geohashPrecision));
        this.assignedIngestMs = assignedIngestMs;
        this.assignedSseMs = assignedSseMs;
        this.enRouteIngestMs = enRouteIngestMs;
        this.enRouteSseMs = enRouteSseMs;
        this.arrivedIngestMs = arrivedIngestMs;
        this.arrivedSseMs = arrivedSseMs;
        this.inProgressIngestMs = inProgressIngestMs;
        this.inProgressSseMs = inProgressSseMs;
    }

    public TrackingLifecycleState resolveState(TrackingLifecycleState incoming,
                                               Double partnerLat,
                                               Double partnerLng,
                                               Double customerLat,
                                               Double customerLng) {
        if (incoming == TrackingLifecycleState.EN_ROUTE && partnerLat != null && partnerLng != null && customerLat != null && customerLng != null) {
            if (isArrived(partnerLat, partnerLng, customerLat, customerLng)) {
                return TrackingLifecycleState.ARRIVED;
            }
        }
        return incoming;
    }

    public boolean isTerminal(TrackingLifecycleState state) {
        return state == TrackingLifecycleState.JOB_COMPLETED
                || state == TrackingLifecycleState.CANCELLED
                || state == TrackingLifecycleState.OFFLINE;
    }

    public long ingestMinIntervalMs(TrackingLifecycleState state) {
        return switch (state) {
            case EN_ROUTE -> enRouteIngestMs;
            case ARRIVED -> arrivedIngestMs;
            case JOB_STARTED, JOB_IN_PROGRESS -> inProgressIngestMs;
            case ASSIGNED -> assignedIngestMs;
            case JOB_COMPLETED, CANCELLED, OFFLINE -> 60_000L;
        };
    }

    public long sseIntervalMs(TrackingLifecycleState state) {
        return switch (state) {
            case EN_ROUTE -> enRouteSseMs;
            case ARRIVED -> arrivedSseMs;
            case JOB_STARTED, JOB_IN_PROGRESS -> inProgressSseMs;
            case ASSIGNED -> assignedSseMs;
            case JOB_COMPLETED, CANCELLED, OFFLINE -> 30_000L;
        };
    }

    public void persistState(Long partnerId, TrackingLifecycleState state, Long bookingId) {
        if (partnerId == null) {
            return;
        }
        String key = "tracking:state:" + partnerId;
        String value = bookingId == null ? state.name() : state.name() + ":" + bookingId;
        redisTemplate.opsForValue().set(key, value, Duration.ofHours(6));
    }

    public TrackingLifecycleState fetchState(Long partnerId) {
        if (partnerId == null) {
            return TrackingLifecycleState.OFFLINE;
        }
        String raw = redisTemplate.opsForValue().get("tracking:state:" + partnerId);
        if (raw == null || raw.isBlank()) {
            return TrackingLifecycleState.OFFLINE;
        }
        String token = raw.contains(":") ? raw.substring(0, raw.indexOf(':')) : raw;
        return TrackingLifecycleState.from(token, TrackingLifecycleState.OFFLINE);
    }

    public TrackingLifecycleState deriveFromOrder(String bookingTypeRaw, OrderStatus orderStatus, TrackingLifecycleState fallback) {
        if (orderStatus == null) {
            return fallback;
        }
        BookingType bookingType = parseBookingType(bookingTypeRaw);
        return switch (orderStatus) {
            case PARTNER_ASSIGNED, PARTNER_ACCEPTED, SEARCHING_PARTNER, PARTNER_SEARCH -> TrackingLifecycleState.ASSIGNED;
            case OUT_FOR_DELIVERY -> TrackingLifecycleState.EN_ROUTE;
            case ARRIVED, PARTNER_REACHED -> TrackingLifecycleState.ARRIVED;
            case SERVICE_STARTED -> TrackingLifecycleState.JOB_STARTED;
            case SERVICE_IN_PROGRESS -> TrackingLifecycleState.JOB_IN_PROGRESS;
            case SERVICE_COMPLETED, DELIVERED -> TrackingLifecycleState.JOB_COMPLETED;
            case CANCELLED, INSTANT_FAILED -> TrackingLifecycleState.CANCELLED;
            case SCHEDULED -> bookingType == BookingType.SCHEDULED ? TrackingLifecycleState.ASSIGNED : fallback;
            default -> fallback;
        };
    }

    private boolean isArrived(double partnerLat, double partnerLng, double customerLat, double customerLng) {
        double meters = GeoDistanceCalculator.haversineMeters(partnerLat, partnerLng, customerLat, customerLng);
        if (meters <= arrivalRadiusMeters) {
            return true;
        }
        String p = GeoHashUtil.encode(partnerLat, partnerLng, geohashPrecision);
        String c = GeoHashUtil.encode(customerLat, customerLng, geohashPrecision);
        return p.equals(c);
    }

    private BookingType parseBookingType(String bookingTypeRaw) {
        if (bookingTypeRaw == null || bookingTypeRaw.isBlank()) {
            return BookingType.INSTANT;
        }
        try {
            return BookingType.valueOf(bookingTypeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BookingType.INSTANT;
        }
    }

}
