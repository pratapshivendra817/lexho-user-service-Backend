package com.lexho.user_service.banner.controller;

import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.service.AdminBannerPromotionService;
import com.lexho.user_service.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.lexho.user_service.admin.service.AdminFileUploadService;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/home-banners", "/api/v1/admin/home-banners"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Banner Promotions", description = "Banner and promotions management APIs for admin")
public class AdminBannerPromotionController {
    private static final Logger log = LoggerFactory.getLogger(AdminBannerPromotionController.class);

    private final AdminBannerPromotionService adminBannerPromotionService;
    private final AdminFileUploadService adminFileUploadService;

    public AdminBannerPromotionController(AdminBannerPromotionService adminBannerPromotionService,
                                          AdminFileUploadService adminFileUploadService) {
        this.adminBannerPromotionService = adminBannerPromotionService;
        this.adminFileUploadService = adminFileUploadService;
    }

    @PostMapping
    @Operation(summary = "Create banner")
    public ResponseEntity<BannerPromotionResponse> create(@Valid @RequestBody BannerUpsertRequest request) {
        return ResponseEntity.ok(adminBannerPromotionService.create(request));
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload banner image")
    public ResponseEntity<Map<String, Object>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        String relativeUrl = adminFileUploadService.uploadBannerImage(file);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
        return ResponseEntity.ok(Map.of("data", Map.of("url", absoluteUrl)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update banner")
    public ResponseEntity<ApiResponse<BannerPromotionResponse>> update(@PathVariable Long id,
                                                                       @Valid @RequestBody BannerUpsertRequest request) {
        log.info("Banner update API called: id={}, title={}, bannerImageUrl={}", id, request.getTitle(), request.getBannerImageUrl());
        BannerPromotionResponse response = adminBannerPromotionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Banner updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete banner")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminBannerPromotionService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get banner by id")
    public ResponseEntity<BannerPromotionResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerPromotionService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all banners with pagination and filters")
    public ResponseEntity<BannerPageResponse<BannerPromotionResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) BannerStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String promoType,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminBannerPromotionService.getAll(page, size, sortBy, sortDir, status, city, promoType, platformType, search));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate banner")
    public ResponseEntity<BannerPromotionResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerPromotionService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate banner")
    public ResponseEntity<BannerPromotionResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerPromotionService.deactivate(id));
    }

    @PatchMapping("/{id}/draft")
    @Operation(summary = "Move banner to draft")
    public ResponseEntity<BannerPromotionResponse> draft(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerPromotionService.moveToDraft(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Banner analytics dashboard stats")
    public ResponseEntity<BannerStatsResponse> stats() {
        return ResponseEntity.ok(adminBannerPromotionService.dashboardStats());
    }
}
