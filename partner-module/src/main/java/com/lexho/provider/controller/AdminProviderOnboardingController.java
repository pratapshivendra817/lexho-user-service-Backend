package com.lexho.provider.controller;

import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderBankDetailsResponse;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.dto.ProviderPageResponse;
import com.lexho.provider.dto.ProviderStatsResponse;
import com.lexho.provider.dto.ProviderStatusReasonRequest;
import com.lexho.provider.dto.ProviderVerificationUpdateRequest;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.exception.ProviderValidationException;
import com.lexho.provider.service.ProviderOnboardingService;
import com.lexho.provider.service.storage.DocumentStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/partners", "/api/admin/partners"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Partner Onboarding", description = "Admin APIs for partner onboarding lifecycle")
public class AdminProviderOnboardingController {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");

    private final ProviderOnboardingService providerOnboardingService;
    private final DocumentStorageService documentStorageService;

    public AdminProviderOnboardingController(ProviderOnboardingService providerOnboardingService,
                                             DocumentStorageService documentStorageService) {
        this.providerOnboardingService = providerOnboardingService;
        this.documentStorageService = documentStorageService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get partner onboarding stats")
    public ResponseEntity<ProviderStatsResponse> stats() {
        return ResponseEntity.ok(providerOnboardingService.getStats());
    }

    @GetMapping
    @Operation(summary = "Get partners list with filters")
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
    @Operation(summary = "Get partner details")
    public ResponseEntity<ProviderDetailsResponse> details(@PathVariable Long id) {
        ProviderDetailsResponse response = providerOnboardingService.getProviderDetails(id);
        enrichDocumentUrls(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/bank-details")
    @Operation(summary = "Get full bank details for partner (admin only)")
    public ResponseEntity<ProviderBankDetailsResponse> bankDetails(@PathVariable Long id) {
        Provider provider = providerOnboardingService.getProviderForAdmin(id);
        String actor = SecurityContextHolder.getContext().getAuthentication() == null
                ? "unknown"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        AUDIT_LOGGER.info(
                "actor={} action=ADMIN_PARTNER_BANK_DETAILS_VIEWED providerId={} providerCode={}",
                actor, provider.getId(), provider.getProviderCode()
        );
        return ResponseEntity.ok(new ProviderBankDetailsResponse(
                provider.getId(),
                provider.getProviderCode(),
                provider.getAccountNumber(),
                provider.getIfscCode()
        ));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve partner")
    public ResponseEntity<ProviderActionResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(providerOnboardingService.approveProvider(id));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject partner")
    public ResponseEntity<ProviderActionResponse> reject(@PathVariable Long id,
                                                         @Valid @RequestBody ProviderStatusReasonRequest request) {
        return ResponseEntity.ok(providerOnboardingService.rejectProvider(id, request.getReason()));
    }

    @PatchMapping("/{id}/verification")
    @Operation(summary = "Legacy verification endpoint (compatibility)")
    public ResponseEntity<ProviderActionResponse> updateVerification(@PathVariable Long id,
                                                                     @Valid @RequestBody ProviderVerificationUpdateRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        ProviderActionResponse response = switch (status) {
            case "VERIFIED", "APPROVED" -> providerOnboardingService.approveProvider(id);
            case "REJECTED" -> providerOnboardingService.rejectProvider(
                    id,
                    request.getReason() == null || request.getReason().isBlank()
                            ? "Rejected by admin"
                            : request.getReason().trim()
            );
            case "BLOCKED" -> providerOnboardingService.blockProvider(
                    id,
                    request.getReason() == null || request.getReason().isBlank()
                            ? "Blocked by admin"
                            : request.getReason().trim()
            );
            case "UNBLOCKED" -> providerOnboardingService.unblockProvider(id);
            default -> throw new ProviderValidationException("Unsupported verification status: " + request.getStatus());
        };
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block partner")
    public ResponseEntity<ProviderActionResponse> block(@PathVariable Long id,
                                                        @Valid @RequestBody ProviderStatusReasonRequest request) {
        return ResponseEntity.ok(providerOnboardingService.blockProvider(id, request.getReason()));
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock partner")
    public ResponseEntity<ProviderActionResponse> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(providerOnboardingService.unblockProvider(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete partner")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        providerOnboardingService.softDeleteProvider(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{providerId}/documents/{documentId}/preview")
    @Operation(summary = "Preview partner document")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long providerId, @PathVariable Long documentId) {
        ProviderDocument providerDocument = providerOnboardingService.getProviderDocument(providerId, documentId);
        Resource resource = documentStorageService.load(providerDocument.getFileImage());
        MediaType mediaType = resolveMediaType(providerDocument.getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(providerDocument.getFileName()).build().toString()
                )
                .body(resource);
    }

    @GetMapping("/{providerId}/documents/{documentId}/download")
    @Operation(summary = "Download partner document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long providerId, @PathVariable Long documentId) {
        ProviderDocument providerDocument = providerOnboardingService.getProviderDocument(providerId, documentId);
        Resource resource = documentStorageService.load(providerDocument.getFileImage());
        MediaType mediaType = resolveMediaType(providerDocument.getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(providerDocument.getFileName()).build().toString()
                )
                .body(resource);
    }

    private void enrichDocumentUrls(ProviderDetailsResponse response) {
        if (response.getDocuments() == null || response.getDocuments().isEmpty()) {
            return;
        }
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        for (var document : response.getDocuments()) {
            String baseDocumentUrl = baseUrl + "/api/admin/partners/" + response.getId() + "/documents/" + document.getId();
            document.setPreviewUrl(baseDocumentUrl + "/preview");
            document.setDownloadUrl(baseDocumentUrl + "/download");
        }
    }

    private MediaType resolveMediaType(String fileType) {
        try {
            return MediaType.parseMediaType(fileType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
