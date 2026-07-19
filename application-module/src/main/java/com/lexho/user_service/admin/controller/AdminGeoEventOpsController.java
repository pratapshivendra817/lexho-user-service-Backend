package com.lexho.user_service.admin.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.geo.events.GeoEventDlqReplayService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/geo/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGeoEventOpsController {
    private final GeoEventDlqReplayService dlqReplayService;

    public AdminGeoEventOpsController(GeoEventDlqReplayService dlqReplayService) {
        this.dlqReplayService = dlqReplayService;
    }

    @PostMapping("/dlq/replay")
    public ApiResponse<Map<String, Object>> replayDlq(@RequestParam(defaultValue = "100") int maxMessages) {
        int replayed = dlqReplayService.replay(maxMessages);
        return ApiResponse.success("DLQ replay completed", Map.of("replayed", replayed, "requested", maxMessages));
    }
}

