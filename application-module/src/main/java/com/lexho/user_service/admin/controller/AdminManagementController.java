package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.admin.service.AdminActivityFeedService;
import com.lexho.user_service.admin.service.AdminFileUploadService;
import com.lexho.user_service.admin.service.AdminManagementService;
import com.lexho.user_service.admin.service.AdminSupportFaqService;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.service.AdminCustomerBannerService;
import com.lexho.user_service.service.ApiRateLimiterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final AdminCatalogService adminCatalogService;
    private final AdminFileUploadService adminFileUploadService;
    private final AdminCustomerBannerService adminCustomerBannerService;
    private final AdminSupportFaqService adminSupportFaqService;
    private final ApiRateLimiterService apiRateLimiterService;
    private final AdminActivityFeedService adminActivityFeedService;
    private final PartnerOrderService partnerOrderService;

    public AdminManagementController(AdminManagementService adminManagementService,
                                     AdminCatalogService adminCatalogService,
                                     AdminFileUploadService adminFileUploadService,
                                     AdminCustomerBannerService adminCustomerBannerService,
                                     AdminSupportFaqService adminSupportFaqService,
                                     ApiRateLimiterService apiRateLimiterService) {
        this(adminManagementService, adminCatalogService, adminFileUploadService, adminCustomerBannerService,
                adminSupportFaqService, apiRateLimiterService, Optional.empty(), Optional.empty());
    }

    @Autowired
    public AdminManagementController(AdminManagementService adminManagementService,
                                     AdminCatalogService adminCatalogService,
                                     AdminFileUploadService adminFileUploadService,
                                     AdminCustomerBannerService adminCustomerBannerService,
                                     AdminSupportFaqService adminSupportFaqService,
                                     ApiRateLimiterService apiRateLimiterService,
                                     Optional<AdminActivityFeedService> adminActivityFeedService,
                                     Optional<PartnerOrderService> partnerOrderService) {
        this.adminManagementService = adminManagementService;
        this.adminCatalogService = adminCatalogService;
        this.adminFileUploadService = adminFileUploadService;
        this.adminCustomerBannerService = adminCustomerBannerService;
        this.adminSupportFaqService = adminSupportFaqService;
        this.apiRateLimiterService = apiRateLimiterService;
        this.adminActivityFeedService = adminActivityFeedService.orElse(null);
        this.partnerOrderService = partnerOrderService.orElse(null);
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMessageResponse deleteService(@PathVariable Long id) {
        return adminCatalogService.deleteService(id);
    }

    @PostMapping("/services/{serviceId}/details")
    public AdminServiceDetailBulkCreateResponse addServiceDetails(@PathVariable Long serviceId,
                                                                  @Valid @RequestBody AdminServiceDetailBulkRequest request) {
        return adminCatalogService.addServiceDetails(serviceId, request);
    }

    @PutMapping("/services/{serviceId}/details/{detailId}")
    public DataResponse<AdminServiceDetailResponse> updateServiceDetail(@PathVariable Long serviceId,
                                                                        @PathVariable Long detailId,
                                                                        @Valid @RequestBody AdminServiceDetailUpdateRequest request) {
        return adminCatalogService.updateServiceDetail(serviceId, detailId, request);
    }

    @DeleteMapping("/services/{serviceId}/details/{detailId}")
    public AdminMessageResponse deleteServiceDetail(@PathVariable Long serviceId,
                                                    @PathVariable Long detailId) {
        return adminCatalogService.deleteServiceDetail(serviceId, detailId);
    }

    @GetMapping("/debug/media-snapshot")
    public DataResponse<Map<String, Object>> mediaSnapshot(@RequestParam(defaultValue = "20") int limit) {
        return adminCatalogService.mediaDebugSnapshot(limit);
    }

    @GetMapping("/debug/media-latest")
    public DataResponse<Map<String, Object>> mediaLatest(@RequestParam(defaultValue = "20") int limit) {
        return adminCatalogService.mediaDebugLatestActive(limit);
    }

    @PostMapping({"/customer-banners", "/banners"})
    public DataResponse<AdminCustomerBannerResponse> createCustomerBanner(@Valid @RequestBody AdminCustomerBannerUpsertRequest request) {
        return adminCustomerBannerService.create(request);
    }

    @PutMapping({"/customer-banners/{id}", "/banners/{id}"})
    public DataResponse<AdminCustomerBannerResponse> updateCustomerBanner(@PathVariable Long id,
                                                                          @Valid @RequestBody AdminCustomerBannerUpsertRequest request) {
        return adminCustomerBannerService.update(id, request);
    }

    @GetMapping({"/customer-banners/{id}", "/banners/{id}"})
    public DataResponse<AdminCustomerBannerResponse> customerBannerById(@PathVariable Long id) {
        return adminCustomerBannerService.byId(id);
    }

    @GetMapping({"/customer-banners", "/banners"})
    public PagedDataResponse<AdminCustomerBannerResponse> customerBanners(@RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "10") int limit,
                                                                           @RequestParam(required = false) Long categoryId,
                                                                           @RequestParam(required = false) Boolean active) {
        return adminCustomerBannerService.list(page, limit, categoryId, active);
    }

    @PatchMapping({"/customer-banners/{id}/status", "/banners/{id}/status"})
    public DataResponse<AdminCustomerBannerResponse> customerBannerStatus(@PathVariable Long id,
                                                                          @RequestParam boolean active) {
        return adminCustomerBannerService.updateStatus(id, active);
    }

    @DeleteMapping({"/customer-banners/{id}", "/banners/{id}"})
    public AdminMessageResponse deleteCustomerBanner(@PathVariable Long id) {
        return adminCustomerBannerService.delete(id);
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        String relativeUrl = adminFileUploadService.uploadServiceImage(file);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
        return Map.of("data", Map.of("url", absoluteUrl));
    }

    @PostMapping(value = "/banners/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadBannerImageForCustomerBanners(@RequestParam("file") MultipartFile file) {
        String relativeUrl = adminFileUploadService.uploadBannerImage(file);
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
        apiRateLimiterService.check("admin_payouts", "global", 60, Duration.ofMinutes(1),
                "Too many payout queries. Please retry shortly");
        return adminManagementService.payouts(page, limit, search, status);
    }

    @PostMapping("/orders/{id}/complete")
    public DataResponse<OrderResponseDto> adminCompleteOrder(@RequestAttribute("userId") Long adminId,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody AdminOrderCompleteRequest request) {
        if (partnerOrderService == null) {
            throw new IllegalStateException("Partner order workflow is not available");
        }
        return new DataResponse<>(partnerOrderService.adminCompleteOrder(adminId, id, request.getReason()));
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
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String categoryId,
                                                                  @RequestParam(required = false) String priority) {
        return adminManagementService.supportTickets(page, limit, search, status, categoryId, priority);
    }

    @GetMapping("/support/customer/tickets")
    public PagedDataResponse<Map<String, Object>> customerSupportTickets(@RequestParam(defaultValue = "1") int page,
                                                                         @RequestParam(defaultValue = "10") int limit,
                                                                         @RequestParam(required = false) String search,
                                                                         @RequestParam(required = false) String status,
                                                                         @RequestParam(required = false) String categoryId,
                                                                         @RequestParam(required = false) String priority) {
        return adminManagementService.customerSupportTickets(page, limit, search, status, categoryId, priority);
    }

    @GetMapping("/support/partner/tickets")
    public PagedDataResponse<Map<String, Object>> partnerSupportTickets(@RequestParam(defaultValue = "1") int page,
                                                                        @RequestParam(defaultValue = "10") int limit,
                                                                        @RequestParam(required = false) String search,
                                                                        @RequestParam(required = false) String status,
                                                                        @RequestParam(required = false) String categoryId,
                                                                        @RequestParam(required = false) String priority) {
        return adminManagementService.partnerSupportTickets(page, limit, search, status, categoryId, priority);
    }

    @GetMapping("/support/tickets/{id}")
    public DataResponse<Map<String, Object>> supportTicketDetail(@PathVariable Long id) {
        return adminManagementService.supportTicketDetail(id);
    }

    @GetMapping("/support/tickets/{id}/stream")
    public SseEmitter supportTicketStream(@PathVariable Long id) {
        return adminManagementService.supportTicketStream(id);
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

    @GetMapping("/inbox/activity")
    public DataResponse<Map<String, Object>> inboxActivity(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int limit) {
        if (adminActivityFeedService == null) {
            return new DataResponse<>(Map.of(
                    "data", java.util.List.of(),
                    "meta", Map.of(
                            "page", Math.max(page, 1),
                            "limit", Math.min(Math.max(limit, 1), 100),
                            "total", 0,
                            "hasMore", false
                    )
            ));
        }
        return new DataResponse<>(adminActivityFeedService.activity(page, limit));
    }

    @GetMapping("/support/faq-categories")
    public DataResponse<java.util.List<Map<String, Object>>> faqCategories() {
        return adminSupportFaqService.listCategories();
    }

    @PostMapping("/support/faq-categories")
    public DataResponse<Map<String, Object>> createFaqCategory(@Valid @RequestBody AdminSupportFaqCategoryUpsertRequest request) {
        return adminSupportFaqService.createCategory(request);
    }

    @PutMapping("/support/faq-categories/{categoryId}")
    public DataResponse<Map<String, Object>> updateFaqCategory(@PathVariable String categoryId,
                                                                @Valid @RequestBody AdminSupportFaqCategoryUpsertRequest request) {
        return adminSupportFaqService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/support/faq-categories/{categoryId}")
    public DataResponse<Map<String, Object>> deleteFaqCategory(@PathVariable String categoryId) {
        return adminSupportFaqService.deleteCategory(categoryId);
    }

    @GetMapping("/support/faq-categories/{categoryId}/faqs")
    public DataResponse<java.util.List<Map<String, Object>>> faqs(@PathVariable String categoryId) {
        return adminSupportFaqService.listFaqs(categoryId);
    }

    @PostMapping("/support/faq-categories/{categoryId}/faqs")
    public DataResponse<Map<String, Object>> createFaq(@PathVariable String categoryId,
                                                       @Valid @RequestBody AdminSupportFaqUpsertRequest request) {
        return adminSupportFaqService.createFaq(categoryId, request);
    }

    @PutMapping("/support/faq-categories/{categoryId}/faqs/{faqId}")
    public DataResponse<Map<String, Object>> updateFaq(@PathVariable String categoryId,
                                                       @PathVariable String faqId,
                                                       @Valid @RequestBody AdminSupportFaqUpsertRequest request) {
        return adminSupportFaqService.updateFaq(categoryId, faqId, request);
    }

    @DeleteMapping("/support/faq-categories/{categoryId}/faqs/{faqId}")
    public DataResponse<Map<String, Object>> deleteFaq(@PathVariable String categoryId,
                                                       @PathVariable String faqId) {
        return adminSupportFaqService.deleteFaq(categoryId, faqId);
    }
}
