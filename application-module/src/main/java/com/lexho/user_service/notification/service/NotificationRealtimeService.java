package com.lexho.user_service.notification.service;

import com.lexho.user_service.util.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
public class NotificationRealtimeService {

    private static final String ADMIN_CHANNEL = "ADMIN";

    private final SseEmitterRegistry<Long> emittersByUserId = new SseEmitterRegistry<>();
    private final SseEmitterRegistry<String> adminEmitters = new SseEmitterRegistry<>();

    public SseEmitter subscribe(Long userId) {
        return emittersByUserId.subscribe(userId, "connected", Map.of("userId", userId));
    }

    public SseEmitter subscribeAdmin() {
        return adminEmitters.subscribe(ADMIN_CHANNEL, "connected", Map.of("userType", ADMIN_CHANNEL));
    }

    public void publish(Long userId, String eventName, Map<String, Object> payload) {
        emittersByUserId.publish(userId, eventName, payload);
    }

    public void publishAdmin(String eventName, Map<String, Object> payload) {
        adminEmitters.publish(ADMIN_CHANNEL, eventName, payload);
    }
}
