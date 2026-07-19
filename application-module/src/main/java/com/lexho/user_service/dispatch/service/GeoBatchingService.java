package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class GeoBatchingService {
    private final CurrentPartnerLocationRepositoryAdapter locationRepositoryAdapter;
    private final GeoEventPublisher geoEventPublisher;
    private final ConcurrentLinkedQueue<Long> geoPointRefreshQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<QueuedMoveEvent> moveEventQueue = new ConcurrentLinkedQueue<>();
    private final Counter mysqlGeoWritesCounter;
    private final Counter rabbitGeoEventsCounter;

    public GeoBatchingService(CurrentPartnerLocationRepositoryAdapter locationRepositoryAdapter,
                              GeoEventPublisher geoEventPublisher,
                              MeterRegistry meterRegistry,
                              @Value("${app.tracking.batch.flush-ms:2000}") long flushMs) {
        this.locationRepositoryAdapter = locationRepositoryAdapter;
        this.geoEventPublisher = geoEventPublisher;
        this.mysqlGeoWritesCounter = Counter.builder("dispatch.geo.mysql_writes").register(meterRegistry);
        this.rabbitGeoEventsCounter = Counter.builder("dispatch.geo.rabbit_events").register(meterRegistry);
    }

    public void queueGeoPointRefresh(Long partnerId) {
        if (partnerId != null) {
            geoPointRefreshQueue.offer(partnerId);
        }
    }

    public void queuePartnerMoved(Long partnerId, Map<String, Object> payload) {
        if (partnerId != null && payload != null) {
            moveEventQueue.offer(new QueuedMoveEvent(partnerId, new HashMap<>(payload)));
        }
    }

    public void publishImmediate(GeoEventType type, Long partnerId, Map<String, Object> payload) {
        geoEventPublisher.publish(type, partnerId, payload);
        rabbitGeoEventsCounter.increment();
    }

    @Scheduled(fixedDelayString = "${app.tracking.batch.flush-ms:2000}")
    public void flush() {
        flushGeoPointRefreshes();
        flushMoveEvents();
    }

    private void flushGeoPointRefreshes() {
        Set<Long> partnerIds = new HashSet<>();
        Long partnerId;
        while ((partnerId = geoPointRefreshQueue.poll()) != null) {
            partnerIds.add(partnerId);
        }
        for (Long id : partnerIds) {
            locationRepositoryAdapter.refreshGeoPoint(id);
            mysqlGeoWritesCounter.increment();
        }
    }

    private void flushMoveEvents() {
        List<QueuedMoveEvent> events = new ArrayList<>();
        QueuedMoveEvent event;
        while ((event = moveEventQueue.poll()) != null) {
            events.add(event);
        }
        for (QueuedMoveEvent queued : events) {
            geoEventPublisher.publish(GeoEventType.PARTNER_MOVED, queued.partnerId(), queued.payload());
            rabbitGeoEventsCounter.increment();
        }
    }

    public interface CurrentPartnerLocationRepositoryAdapter {
        void refreshGeoPoint(Long partnerId);
    }

    private record QueuedMoveEvent(Long partnerId, Map<String, Object> payload) {
    }
}
