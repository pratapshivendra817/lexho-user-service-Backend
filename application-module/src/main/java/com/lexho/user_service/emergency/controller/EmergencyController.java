package com.lexho.user_service.emergency.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.emergency.dto.EmergencyContactRequest;
import com.lexho.user_service.emergency.dto.EmergencyContactResponse;
import com.lexho.user_service.emergency.dto.EmergencyEventResponse;
import com.lexho.user_service.emergency.dto.EmergencySosRequest;
import com.lexho.user_service.emergency.service.EmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/emergency", "/api/v1/emergency"})
@PreAuthorize("hasRole('PARTNER')")
@Tag(name = "Partner Emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @PostMapping("/sos")
    @Operation(summary = "Trigger partner SOS emergency incident")
    public ApiResponse<EmergencyEventResponse> triggerSos(@RequestAttribute("userId") Long partnerId,
                                                          @Valid @RequestBody EmergencySosRequest request) {
        return ApiResponse.success("Emergency SOS triggered", emergencyService.triggerSos(partnerId, request));
    }

    @GetMapping("/history")
    @Operation(summary = "Get partner emergency history")
    public ApiResponse<List<EmergencyEventResponse>> history(@RequestAttribute("userId") Long partnerId) {
        return ApiResponse.success("Emergency history fetched", emergencyService.history(partnerId));
    }

    @PostMapping("/contact")
    @Operation(summary = "Add emergency contact for partner")
    public ApiResponse<EmergencyContactResponse> addContact(@RequestAttribute("userId") Long partnerId,
                                                            @Valid @RequestBody EmergencyContactRequest request) {
        return ApiResponse.success("Emergency contact saved", emergencyService.addContact(partnerId, request));
    }
}
