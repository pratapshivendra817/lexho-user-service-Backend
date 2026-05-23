package com.lexho.user_service.customer.location.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "lexho-user-service",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "ok", true,
                "service", "lexho-user-service",
                "timestamp", Instant.now().toString()
        );
    }
}
