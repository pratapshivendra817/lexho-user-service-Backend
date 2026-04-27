package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.admin.service.AdminFileUploadService;
import com.lexho.user_service.admin.service.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final AdminCatalogService adminCatalogService;
    private final AdminFileUploadService adminFileUploadService;

    public AdminManagementController(AdminManagementService adminManagementService,
                                     AdminCatalogService adminCatalogService,
                                     AdminFileUploadService adminFileUploadService) {
        this.adminManagementService = adminManagementService;
        this.adminCatalogService = adminCatalogService;
        this.adminFileUploadService = adminFileUploadService;
    }

    @PostMapping("/categories")
    public DataResponse<AdminCategoryResponse> createCategory(@Valid @RequestBody AdminCategoryUpsertRequest request) {
        return adminCatalogService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public DataResponse<AdminCategoryResponse> updateCategory(@PathVariable Long id,
                                                              @Valid @RequestBody AdminCategoryUpsertRequest request) {
        return adminCatalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminMessageResponse deleteCategory(@PathVariable Long id) {
        return adminCatalogService.deleteCategory(id);
    }

    @GetMapping("/categories")
    public PagedDataResponse<AdminCategoryResponse> categories(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int limit,
                                                               @RequestParam(required = false) String search) {
        return adminCatalogService.listCategories(page, limit, search);
    }

    @PatchMapping("/categories/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DataResponse<AdminCategoryResponse> updateCategoryStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody AdminCategoryStatusRequest request) {
        return adminCatalogService.updateCategoryStatus(id, request.getActive());
    }

    @PostMapping("/services")
    public DataResponse<AdminServiceResponse> createService(@Valid @RequestBody AdminServiceUpsertRequest request) {
        return adminCatalogService.createService(request);
    }

    @PutMapping("/services/{id}")
    public DataResponse<AdminServiceResponse> updateService(@PathVariable Long id,
                                                            @Valid @RequestBody AdminServiceUpsertRequest request) {
        return adminCatalogService.updateService(id, request);
    }

    @GetMapping("/services")
    public PagedDataResponse<AdminServiceResponse> services(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int limit,
                                                            @RequestParam(required = false) String search,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String sort) {
        return adminCatalogService.listServices(page, limit, search, category, status, sort);
    }

    @GetMapping("/services/{id}")
    public DataResponse<AdminServiceResponse> serviceById(@PathVariable Long id) {
        return adminCatalogService.getServiceById(id);
    }

    @PatchMapping("/services/{id}/toggle-status")
    public DataResponse<AdminServiceResponse> toggleServiceStatus(@PathVariable Long id) {
        return adminCatalogService.toggleServiceStatus(id);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminMessageResponse deleteService(@PathVariable Long id) {
        return adminCatalogService.deleteService(id);
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        String relativeUrl = adminFileUploadService.uploadServiceImage(file);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
        return Map.of("data", Map.of("url", absoluteUrl));
    }

    @GetMapping("/payouts")
    public PagedDataResponse<Map<String, Object>> payouts(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int limit,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) String status) {
        return adminManagementService.payouts(page, limit, search, status);
    }

    @GetMapping("/activity-logs")
    public PagedDataResponse<Map<String, Object>> activityLogs(@RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "10") int limit,
                                                                @RequestParam(required = false) String search) {
        return adminManagementService.activityLogs(page, limit, search);
    }

    @GetMapping("/support/tickets")
    public PagedDataResponse<Map<String, Object>> supportTickets(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int limit,
                                                                  @RequestParam(required = false) String search,
                                                                  @RequestParam(required = false) String status) {
        return adminManagementService.supportTickets(page, limit, search, status);
    }

    @PatchMapping("/support/tickets/{id}/status")
    public DataResponse<Map<String, Object>> supportTicketStatus(@PathVariable Long id,
                                                                  @Valid @RequestBody AdminSupportTicketStatusRequest request) {
        return adminManagementService.updateSupportTicketStatus(id, request.getStatus());
    }

    @PostMapping("/support/tickets/{id}/reply")
    public DataResponse<Map<String, Object>> supportTicketReply(@PathVariable Long id,
                                                                 @Valid @RequestBody AdminSupportTicketReplyRequest request) {
        return adminManagementService.replySupportTicket(id, request.getMessage());
    }
}
