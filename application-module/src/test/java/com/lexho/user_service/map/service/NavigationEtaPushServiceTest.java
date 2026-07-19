package com.lexho.user_service.map.service;

import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.map.dto.NavigationEtaUpdateMessage;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NavigationEtaPushServiceTest {

    @Test
    void publishesEtaToWebsocketTopicsAndGeoEvents() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GeoEventPublisher geoEventPublisher = mock(GeoEventPublisher.class);
        NavigationEtaPushService service = new NavigationEtaPushService(messagingTemplate, geoEventPublisher);
        NavigationRouteResponse response = new NavigationRouteResponse(
                77L, 4.2, 12, "poly", LocalDateTime.of(2026, 6, 14, 20, 45),
                true, "MAPBOX", false, true, false, null, 2, true,
                28.61, 77.20, LocalDateTime.of(2026, 6, 14, 20, 33),
                28.62, 77.21, LocalDateTime.of(2026, 6, 14, 20, 34)
        );

        service.publish(77L, response);

        verify(messagingTemplate).convertAndSend(eq("/topic/navigation/eta/77"), any(NavigationEtaUpdateMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/bookings/77/eta"), any(NavigationEtaUpdateMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/customer/bookings/77/eta"), any(NavigationEtaUpdateMessage.class));
        verify(geoEventPublisher).publish(any(), eq(77L), any());
    }
}
