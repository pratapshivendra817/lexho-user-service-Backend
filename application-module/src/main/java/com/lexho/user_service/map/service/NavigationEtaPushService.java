package com.lexho.user_service.map.service;

import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.map.dto.NavigationEtaUpdateMessage;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NavigationEtaPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final GeoEventPublisher geoEventPublisher;

    public NavigationEtaPushService(SimpMessagingTemplate messagingTemplate,
                                    GeoEventPublisher geoEventPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.geoEventPublisher = geoEventPublisher;
    }

    public void publish(Long bookingId, NavigationRouteResponse response) {
        if (bookingId == null || response == null) {
            return;
        }

        NavigationEtaUpdateMessage payload = NavigationEtaUpdateMessage.from(response);

        messagingTemplate.convertAndSend("/topic/navigation/eta/" + bookingId, payload);
        messagingTemplate.convertAndSend("/topic/admin/bookings/" + bookingId + "/eta", payload);
        messagingTemplate.convertAndSend("/topic/customer/bookings/" + bookingId + "/eta", payload);
        geoEventPublisher.publish(GeoEventType.NAVIGATION_ETA_UPDATED, bookingId, Map.of(
                "bookingId", payload.bookingId(),
                "distanceKm", payload.distanceKm(),
                "durationMinutes", payload.durationMinutes(),
                "eta", String.valueOf(payload.eta()),
                "routeDeviationDetected", payload.routeDeviationDetected(),
                "routeRecalculated", payload.routeRecalculated(),
                "provider", payload.provider()
        ));
    }
}
