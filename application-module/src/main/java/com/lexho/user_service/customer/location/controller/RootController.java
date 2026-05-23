package com.lexho.user_service.customer.location.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping({"", "/"})
    public ResponseEntity<Void> root() {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/api/health"))
                .build();
    }

    @GetMapping("/ping")
    @ResponseBody
    public Map<String, Object> ping() {
        return Map.of(
                "ok", true,
                "service", "lexho-user-service",
                "timestamp", Instant.now().toString()
        );
    }
}
