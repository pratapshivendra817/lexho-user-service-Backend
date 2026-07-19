package com.lexho.provider.controller;

import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentResponse;
import com.lexho.provider.dto.ProviderDocumentUploadResponse;
import com.lexho.provider.dto.ProviderOnboardingRequest;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.provider.mapper.ProviderMapper;
import com.lexho.provider.service.ProviderOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/partner")
@Tag(name = "Partner Onboarding", description = "Partner app onboarding and documents APIs")
public class ProviderOnboardingController {

    private final ProviderOnboardingService providerOnboardingService;
    private final ProviderMapper providerMapper;

    public ProviderOnboardingController(ProviderOnboardingService providerOnboardingService,
                                        ProviderMapper providerMapper) {
        this.providerOnboardingService = providerOnboardingService;
        this.providerMapper = providerMapper;
    }

    @PostMapping("/onboarding")
    @Operation(summary = "Submit partner onboarding request")
    public ResponseEntity<ProviderDetailsResponse> onboarding(@Valid @RequestBody ProviderOnboardingRequest request) {
        return ResponseEntity.ok(providerOnboardingService.submitOnboarding(request));
    }

    @PostMapping("/documents/upload")
    @PreAuthorize("hasRole('PARTNER')")
    @Operation(summary = "Upload partner document")
    public ResponseEntity<ProviderDocumentUploadResponse> uploadDocument(@RequestParam Long providerId,
                                                                         @RequestParam ProviderDocumentType documentType,
                                                                         @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(providerOnboardingService.uploadDocument(providerId, documentType, file));
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    @Operation(summary = "Get partner document metadata")
    public ResponseEntity<ProviderDocumentResponse> getDocument(@PathVariable Long id) {
        ProviderDocument document = providerOnboardingService.getDocument(id);
        return ResponseEntity.ok(providerMapper.toDocument(document));
    }
}
