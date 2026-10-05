package com.lexho.user_service.legal.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.legal.dto.LegalAcceptRequest;
import com.lexho.user_service.legal.dto.LegalAcceptanceResponse;
import com.lexho.user_service.legal.service.LegalAcceptanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalAcceptanceController {
    private final LegalAcceptanceService service;

    public LegalAcceptanceController(LegalAcceptanceService service) {
        this.service = service;
    }

    @PostMapping("/accept")
    public ApiResponse<LegalAcceptanceResponse> accept(@RequestAttribute("userId") Long userId,
                                                       @RequestAttribute("role") String role,
                                                       @Valid @RequestBody LegalAcceptRequest request) {
        return ApiResponse.success("Legal acceptance saved", service.accept(userId, role, request));
    }

    @GetMapping("/acceptance-history")
    public ApiResponse<List<LegalAcceptanceResponse>> history(@RequestAttribute("userId") Long userId,
                                                              @RequestAttribute("role") String role) {
        return ApiResponse.success("Legal acceptance history fetched", service.history(userId, role));
    }
}
