package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GeoBatchingServiceTest {

    private final GeoBatchingService.CurrentPartnerLocationRepositoryAdapter adapter =
            mock(GeoBatchingService.CurrentPartnerLocationRepositoryAdapter.class);
    private final GeoEventPublisher publisher = mock(GeoEventPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final GeoBatchingService service = new GeoBatchingService(adapter, publisher, registry, 2_000);

    @Test
    void flushDeduplicatesRefreshesAndPublishesCopiedMoves() {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of("lat", 28.6));
        service.queueGeoPointRefresh(1L);
        service.queueGeoPointRefresh(1L);
        service.queuePartnerMoved(2L, payload);
        payload.put("lat", 0);

        service.flush();

        verify(adapter).refreshGeoPoint(1L);
        verify(publisher).publish(GeoEventType.PARTNER_MOVED, 2L, Map.of("lat", 28.6));
        assertThat(registry.get("dispatch.geo.mysql_writes").counter().count()).isEqualTo(1);
        assertThat(registry.get("dispatch.geo.rabbit_events").counter().count()).isEqualTo(1);
    }

    @Test
    void ignoresInvalidQueueInputsAndPublishesImmediateEvent() {
        service.queueGeoPointRefresh(null);
        service.queuePartnerMoved(null, Map.of());
        service.queuePartnerMoved(1L, null);
        service.flush();

        verifyNoInteractions(adapter);
        service.publishImmediate(GeoEventType.PARTNER_ONLINE, 3L, Map.of());
        verify(publisher, times(1)).publish(GeoEventType.PARTNER_ONLINE, 3L, Map.of());
    }
}
