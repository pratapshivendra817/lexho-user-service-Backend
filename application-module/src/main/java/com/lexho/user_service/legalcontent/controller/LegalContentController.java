package com.lexho.user_service.legalcontent.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.legalcontent.dto.LegalDocumentResponse;
import com.lexho.user_service.legalcontent.service.LegalContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegalContentController {
    private static final String LEGAL_DOCUMENT_FETCHED = "Legal document fetched";

    private final LegalContentService legalContentService;

    public LegalContentController(LegalContentService legalContentService) {
        this.legalContentService = legalContentService;
    }

    @GetMapping({"/api/content/documents/{slug}", "/api/v1/content/documents/{slug}"})
    public ApiResponse<LegalDocumentResponse> getPublished(@PathVariable String slug,
                                                           @RequestParam(defaultValue = "PARTNER") String platform) {
        return ApiResponse.success(LEGAL_DOCUMENT_FETCHED, legalContentService.getPublished(slug, platform));
    }

    @GetMapping("/api/v1/legal/{document}")
    public ApiResponse<LegalDocumentResponse> publicDocument(@PathVariable String document) {
        return ApiResponse.success(LEGAL_DOCUMENT_FETCHED, legalContentService.getPublicDocument(document));
    }

    @GetMapping("/api/v1/legal/current")
    public ApiResponse<LegalDocumentResponse> currentByType(@RequestParam String documentType) {
        return ApiResponse.success(LEGAL_DOCUMENT_FETCHED, legalContentService.getPublishedByType(documentType));
    }
}
