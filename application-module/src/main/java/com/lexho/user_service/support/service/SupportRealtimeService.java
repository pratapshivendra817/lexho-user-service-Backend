package com.lexho.user_service.support.service;

import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.util.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
public class SupportRealtimeService {

    private static final String ADMIN_REQUESTER_TYPE = "ADMIN";

    private final SseEmitterRegistry<String> emitters = new SseEmitterRegistry<>();

    public SseEmitter subscribe(String requesterType, Long requesterId, Long ticketId) {
        String channel = channelKey(requesterType, requesterId, ticketId);
        return emitters.subscribe(channel, "connected", Map.of(
                "ticketId", ticketId,
                "requesterType", requesterType
        ));
    }

    public SseEmitter subscribeAdmin(Long ticketId) {
        String channel = adminChannelKey(ticketId);
        return emitters.subscribe(channel, "connected", Map.of(
                "ticketId", ticketId,
                "requesterType", ADMIN_REQUESTER_TYPE
        ));
    }

    public void publish(SupportTicket ticket, String requesterType, String eventName, Map<String, Object> payload) {
        if (ticket == null || ticket.getId() == null || ticket.getUserId() == null) {
            return;
        }
        publishToChannel(channelKey(requesterType, ticket.getUserId(), ticket.getId()), eventName, payload);
        publishToChannel(adminChannelKey(ticket.getId()), eventName, payload);
    }

    private String channelKey(String requesterType, Long requesterId, Long ticketId) {
        return requesterType + ":" + requesterId + ":" + ticketId;
    }

    private String adminChannelKey(Long ticketId) {
        return ADMIN_REQUESTER_TYPE + ":" + ticketId;
    }

    private void publishToChannel(String channel, String eventName, Map<String, Object> payload) {
        emitters.publish(channel, eventName, payload);
    }
}
