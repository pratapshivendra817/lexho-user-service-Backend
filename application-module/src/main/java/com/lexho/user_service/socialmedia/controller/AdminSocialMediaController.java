package com.lexho.user_service.socialmedia.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaPageResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaReorderRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaStatusRequest;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import com.lexho.user_service.socialmedia.service.SocialMediaManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/admin/social-media")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminSocialMediaController {
    private final SocialMediaManagementService socialMediaManagementService;

    @GetMapping
    @Operation(summary = "List social media links for admin")
    public ResponseEntity<ApiResponse<SocialMediaPageResponse>> all(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(required = false) Boolean active,
                                                                    @RequestParam(required = false) SocialMediaPlatform platform) {
        return ResponseEntity.ok(ApiResponse.success("Social media links fetched",
                socialMediaManagementService.getAdminLinks(page, size, active, platform)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a social media link")
    public ResponseEntity<ApiResponse<SocialMediaLinkResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Social media link fetched", socialMediaManagementService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a social media link")
    public ResponseEntity<ApiResponse<SocialMediaLinkResponse>> create(@Valid @RequestBody SocialMediaLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social media link created", socialMediaManagementService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a social media link")
    public ResponseEntity<ApiResponse<SocialMediaLinkResponse>> update(@PathVariable Long id,
                                                                       @Valid @RequestBody SocialMediaLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social media link updated", socialMediaManagementService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable a social media link")
    public ResponseEntity<ApiResponse<SocialMediaLinkResponse>> status(@PathVariable Long id,
                                                                       @Valid @RequestBody SocialMediaStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social media link status updated",
                socialMediaManagementService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a social media link")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        socialMediaManagementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Social media link deleted", null));
    }

    @PatchMapping("/reorder")
    @Operation(summary = "Reorder social media links")
    public ResponseEntity<ApiResponse<List<SocialMediaLinkResponse>>> reorder(
            @RequestBody List<@Valid SocialMediaReorderRequest> request) {
        return ResponseEntity.ok(ApiResponse.success("Social media links reordered",
                socialMediaManagementService.reorder(request)));
    }
}
