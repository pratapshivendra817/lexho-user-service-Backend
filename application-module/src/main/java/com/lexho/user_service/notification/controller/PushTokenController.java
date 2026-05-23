package com.lexho.user_service.notification.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.service.PushTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/devices")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    public PushTokenController(PushTokenService pushTokenService) {
        this.pushTokenService = pushTokenService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> register(@RequestAttribute("userId") Long userId,
                                                      @RequestAttribute("role") String role,
                                                      @Valid @RequestBody PushTokenUpsertRequest request) {
        pushTokenService.upsert(userId, role, request);
        return ApiResponse.success("Push token registered successfully", Map.of("deviceId", request.deviceId));
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> unregister(@RequestAttribute("userId") Long userId,
                                        @RequestAttribute("role") String role,
                                        @PathVariable String deviceId) {
        pushTokenService.deactivate(userId, role, deviceId);
        return ApiResponse.success("Push token removed successfully", null);
    }
}
