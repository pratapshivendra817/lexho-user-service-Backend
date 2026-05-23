package com.lexho.user_service.homebanner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerReorderRequest;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerUpsertRequest;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import com.lexho.user_service.homebanner.service.HomeBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping({"/api/admin/home/banners", "/api/v1/admin/home/banners"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Home Banner Management", description = "Admin APIs to manage customer home banners")
public class AdminHomeBannerController {

    private final HomeBannerService homeBannerService;

    @PostMapping
    @Operation(summary = "Create home banner")
    public ApiResponse<HomeBannerResponse> create(@Valid @RequestBody HomeBannerUpsertRequest request) {
        return ApiResponse.success("Home banner created", homeBannerService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update home banner")
    public ApiResponse<HomeBannerResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody HomeBannerUpsertRequest request) {
        return ApiResponse.success("Home banner updated", homeBannerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete home banner")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        homeBannerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all home banners")
    public ApiResponse<List<HomeBannerResponse>> list(@RequestParam(required = false) HomeBannerAudience audience) {
        return ApiResponse.success("Home banners fetched", homeBannerService.getAdminBanners(audience));
    }

    @PatchMapping("/reorder")
    @Operation(summary = "Reorder home banners")
    public ApiResponse<List<HomeBannerResponse>> reorder(@Valid @RequestBody HomeBannerReorderRequest request) {
        return ApiResponse.success("Home banners reordered", homeBannerService.reorder(request));
    }
}
