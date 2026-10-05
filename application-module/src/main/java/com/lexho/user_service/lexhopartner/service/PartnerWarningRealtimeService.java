package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.util.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
public class PartnerWarningRealtimeService {

    private final SseEmitterRegistry<String> emitters = new SseEmitterRegistry<>();

    public SseEmitter subscribe(Long partnerId, Long warningId) {
        String channel = channelKey(partnerId, warningId);
        return emitters.subscribe(channel, "connected", Map.of("partnerId", partnerId, "warningId", warningId));
    }

    public void publish(Long partnerId, Long warningId, String eventName, Map<String, Object> payload) {
        String channel = channelKey(partnerId, warningId);
        emitters.publish(channel, eventName, payload);
    }

    private String channelKey(Long partnerId, Long warningId) {
        return partnerId + ":" + warningId;
    }
}
