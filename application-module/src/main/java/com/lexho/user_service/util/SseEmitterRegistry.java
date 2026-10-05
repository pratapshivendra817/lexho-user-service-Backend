package com.lexho.user_service.util;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseEmitterRegistry<K> {

    private static final long TIMEOUT_MILLIS = 300_000L;

    private final Map<K, List<SseEmitter>> emittersByKey = new ConcurrentHashMap<>();

    public SseEmitter subscribe(K key, String connectedEventName, Map<String, Object> connectedPayload) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emittersByKey.computeIfAbsent(key, currentKey -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(exception -> remove(key, emitter));
        send(emitter, connectedEventName, connectedPayload);
        return emitter;
    }

    public void publish(K key, String eventName, Map<String, Object> payload) {
        List<SseEmitter> emitters = emittersByKey.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            if (!send(emitter, eventName, payload)) {
                remove(key, emitter);
            }
        }
    }

    private boolean send(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void remove(K key, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByKey.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByKey.remove(key);
        }
    }
}
