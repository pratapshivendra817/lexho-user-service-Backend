package com.lexho.user_service.section.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.service.SectionManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/home/sections")
@RequiredArgsConstructor
public class CustomerSectionController {

    private final SectionManagementService sectionManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Active home sections fetched", sectionManagementService.getActiveCustomerSections()));
    }
}
