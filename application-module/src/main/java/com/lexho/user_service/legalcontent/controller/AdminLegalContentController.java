package com.lexho.user_service.legalcontent.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.legalcontent.dto.LegalDocumentRequest;
import com.lexho.user_service.legalcontent.dto.LegalDocumentResponse;
import com.lexho.user_service.legalcontent.service.LegalContentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/legal-content", "/api/v1/admin/legal-content", "/api/v1/admin/legal"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminLegalContentController {

    private final LegalContentService legalContentService;

    public AdminLegalContentController(LegalContentService legalContentService) {
        this.legalContentService = legalContentService;
    }

    @GetMapping
    public ApiResponse<List<LegalDocumentResponse>> list(@RequestParam(defaultValue = "PARTNER") String platform) {
        return ApiResponse.success("Legal documents fetched", legalContentService.list(platform));
    }

    @GetMapping("/{slug:[a-zA-Z][a-zA-Z0-9-]*}")
    public ApiResponse<LegalDocumentResponse> get(@PathVariable String slug,
                                                  @RequestParam(defaultValue = "PARTNER") String platform) {
        return ApiResponse.success("Legal document fetched", legalContentService.adminGet(slug, platform));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<LegalDocumentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Legal document fetched", legalContentService.getById(id));
    }

    @PostMapping
    public ApiResponse<LegalDocumentResponse> create(@RequestAttribute(value = "userId", required = false) Long adminId,
                                                     @Valid @RequestBody LegalDocumentRequest request) {
        return ApiResponse.success("Legal document created", legalContentService.createDraft(request, adminId));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<LegalDocumentResponse> updateById(@PathVariable Long id,
                                                         @RequestAttribute(value = "userId", required = false) Long adminId,
                                                         @Valid @RequestBody LegalDocumentRequest request) {
        return ApiResponse.success("Legal document updated", legalContentService.updateDraft(id, request, adminId));
    }

    @PostMapping("/{id:\\d+}/publish")
    public ApiResponse<LegalDocumentResponse> publishById(@PathVariable Long id,
                                                          @RequestAttribute(value = "userId", required = false) Long adminId) {
        return ApiResponse.success("Legal document published", legalContentService.publish(id, adminId));
    }

    @PostMapping("/{id:\\d+}/archive")
    public ApiResponse<LegalDocumentResponse> archiveById(@PathVariable Long id,
                                                          @RequestAttribute(value = "userId", required = false) Long adminId) {
        return ApiResponse.success("Legal document archived", legalContentService.archive(id, adminId));
    }

    @GetMapping("/{id:\\d+}/versions")
    public ApiResponse<List<LegalDocumentResponse>> versionsById(@PathVariable Long id) {
        return ApiResponse.success("Legal document versions fetched", legalContentService.versions(id));
    }

    @PutMapping("/{slug:[a-zA-Z][a-zA-Z0-9-]*}")
    public ApiResponse<LegalDocumentResponse> upsert(@PathVariable String slug,
                                                     @RequestAttribute(value = "userId", required = false) Long adminId,
                                                     @Valid @RequestBody LegalDocumentRequest request) {
        return ApiResponse.success("Legal document saved", legalContentService.upsert(slug, request, adminId));
    }
}
