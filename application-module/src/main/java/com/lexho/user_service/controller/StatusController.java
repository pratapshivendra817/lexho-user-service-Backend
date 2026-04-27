package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("timestamp", Instant.now().toString());
        return ApiResponse.success("OK", payload);
    }
}
