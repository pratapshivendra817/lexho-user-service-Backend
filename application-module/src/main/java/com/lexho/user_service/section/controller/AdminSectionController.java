package com.lexho.user_service.section.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.section.dto.SectionCreateRequest;
import com.lexho.user_service.section.dto.SectionItemReorderRequest;
import com.lexho.user_service.section.dto.SectionItemRequest;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionItemStatusRequest;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionReorderRequest;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionStatusRequest;
import com.lexho.user_service.section.dto.SectionUpdateRequest;
import com.lexho.user_service.section.service.SectionManagementService;
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
@RequestMapping("/api/admin/sections")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSectionController {

    private final SectionManagementService sectionManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<SectionResponse>> create(@Valid @RequestBody SectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section created", sectionManagementService.createSection(request)));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<SectionItemResponse>>> addItems(@PathVariable Long id,
                                                                            @RequestBody List<@Valid SectionItemRequest> request) {
        return ResponseEntity.ok(ApiResponse.success("Section items added", sectionManagementService.addSectionItems(id, request)));
    }

    @PutMapping("/{sectionId}/items/{itemId}")
    public ResponseEntity<ApiResponse<SectionItemResponse>> updateItem(@PathVariable Long sectionId,
                                                                       @PathVariable Long itemId,
                                                                       @Valid @RequestBody SectionItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section item updated",
                sectionManagementService.updateSectionItem(sectionId, itemId, request)));
    }

    @DeleteMapping("/{sectionId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long sectionId,
                                                        @PathVariable Long itemId) {
        sectionManagementService.deleteSectionItem(sectionId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Section item deleted", null));
    }

    @PatchMapping("/{sectionId}/items/{itemId}/status")
    public ResponseEntity<ApiResponse<SectionItemResponse>> updateItemStatus(@PathVariable Long sectionId,
                                                                             @PathVariable Long itemId,
                                                                             @Valid @RequestBody SectionItemStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section item status updated",
                sectionManagementService.updateSectionItemStatus(sectionId, itemId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody SectionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section updated", sectionManagementService.updateSection(id, request)));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> reorderSections(
            @RequestBody List<@Valid SectionReorderRequest> request) {
        return ResponseEntity.ok(ApiResponse.success("Sections reordered", sectionManagementService.reorderSections(request)));
    }

    @PatchMapping("/{id}/reorder")
    public ResponseEntity<ApiResponse<List<SectionItemResponse>>> reorder(@PathVariable Long id,
                                                                           @RequestBody List<@Valid SectionItemReorderRequest> request) {
        return ResponseEntity.ok(ApiResponse.success("Section items reordered", sectionManagementService.reorderItems(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SectionResponse>> status(@PathVariable Long id,
                                                               @Valid @RequestBody SectionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section status updated", sectionManagementService.updateSectionStatus(id, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SectionPageResponse>> all(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Sections fetched", sectionManagementService.getSections(page, size)));
    }
}
