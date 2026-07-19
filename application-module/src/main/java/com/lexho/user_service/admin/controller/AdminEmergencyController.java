package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.emergency.dto.EmergencyAdminActionRequest;
import com.lexho.user_service.emergency.dto.EmergencyIncidentPageResponse;
import com.lexho.user_service.emergency.dto.EmergencyEventResponse;
import com.lexho.user_service.emergency.service.EmergencyAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/emergency", "/api/v1/admin/emergency"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmergencyController {

    private final EmergencyAdminService emergencyAdminService;

    public AdminEmergencyController(EmergencyAdminService emergencyAdminService) {
        this.emergencyAdminService = emergencyAdminService;
    }

    @GetMapping("/incidents")
    public DataResponse<EmergencyIncidentPageResponse> incidents(@RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int limit,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) Long partnerId) {
        return emergencyAdminService.incidents(page, limit, status, partnerId);
    }

    @GetMapping("/incidents/{eventId}")
    public DataResponse<EmergencyEventResponse> incident(@PathVariable Long eventId) {
        return emergencyAdminService.incident(eventId);
    }

    @PatchMapping("/incidents/{eventId}/acknowledge")
    public DataResponse<EmergencyEventResponse> acknowledge(@PathVariable Long eventId,
                                                            @Valid @RequestBody(required = false) EmergencyAdminActionRequest request) {
        return emergencyAdminService.acknowledge(eventId, request == null ? null : request.note());
    }

    @PatchMapping("/incidents/{eventId}/resolve")
    public DataResponse<EmergencyEventResponse> resolve(@PathVariable Long eventId,
                                                        @Valid @RequestBody(required = false) EmergencyAdminActionRequest request) {
        return emergencyAdminService.resolve(eventId, request == null ? null : request.note());
    }
}
