package com.lexho.provider.controller;

import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.dto.ProviderPageResponse;
import com.lexho.provider.dto.ProviderStatsResponse;
import com.lexho.provider.dto.ProviderStatusReasonRequest;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.service.ProviderOnboardingService;
import com.lexho.provider.service.storage.DocumentStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Provider Onboarding", description = "Admin APIs for provider onboarding lifecycle")
public class AdminProviderOnboardingController {

    private final ProviderOnboardingService providerOnboardingService;
    private final DocumentStorageService documentStorageService;

    public AdminProviderOnboardingController(ProviderOnboardingService providerOnboardingService,
                                             DocumentStorageService documentStorageService) {
        this.providerOnboardingService = providerOnboardingService;
        this.documentStorageService = documentStorageService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get provider onboarding stats")
    public ResponseEntity<ProviderStatsResponse> stats() {
        return ResponseEntity.ok(providerOnboardingService.getStats());
    }

    @GetMapping
    @Operation(summary = "Get providers list with filters")
    public ResponseEntity<ProviderPageResponse<ProviderListItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(providerOnboardingService.getProviders(
                page, size, sortBy, sortDir, status, serviceType, city, search
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get provider details")
    public ResponseEntity<ProviderDetailsResponse> details(@PathVariable Long id) {
        return ResponseEntity.ok(providerOnboardingService.getProviderDetails(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve provider")
    public ResponseEntity<ProviderActionResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(providerOnboardingService.approveProvider(id));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject provider")
    public ResponseEntity<ProviderActionResponse> reject(@PathVariable Long id,
                                                         @Valid @RequestBody ProviderStatusReasonRequest request) {
        return ResponseEntity.ok(providerOnboardingService.rejectProvider(id, request.getReason()));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block provider")
    public ResponseEntity<ProviderActionResponse> block(@PathVariable Long id,
                                                        @Valid @RequestBody ProviderStatusReasonRequest request) {
        return ResponseEntity.ok(providerOnboardingService.blockProvider(id, request.getReason()));
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock provider")
    public ResponseEntity<ProviderActionResponse> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(providerOnboardingService.unblockProvider(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete provider")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        providerOnboardingService.softDeleteProvider(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{providerId}/documents/{documentId}/download")
    @Operation(summary = "Download provider document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long providerId, @PathVariable Long documentId) {
        ProviderDocument providerDocument = providerOnboardingService.getProviderDocument(providerId, documentId);
        Resource resource = documentStorageService.load(providerDocument.getFileImage());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(providerDocument.getFileType());
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(providerDocument.getFileName()).build().toString())
                .body(resource);
    }
}
