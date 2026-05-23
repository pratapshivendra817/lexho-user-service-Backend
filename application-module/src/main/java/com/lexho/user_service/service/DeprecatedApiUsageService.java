package com.lexho.user_service.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DeprecatedApiUsageService {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSeen = new ConcurrentHashMap<>();

    public void record(String apiKey) {
        counters.computeIfAbsent(apiKey, key -> new AtomicLong()).incrementAndGet();
        lastSeen.put(apiKey, LocalDateTime.now());
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        counters.forEach((key, value) -> {
            Map<String, Object> item = new ConcurrentHashMap<>();
            item.put("count", value.get());
            item.put("lastSeen", lastSeen.get(key));
            stats.put(key, item);
        });
        return stats;
    }
}

