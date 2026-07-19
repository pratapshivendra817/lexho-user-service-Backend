package com.lexho.user_service.admin.controller;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.payment.dto.AdminWebhookEventListResponse;
import com.lexho.user_service.payment.dto.AdminWebhookEventResponse;
import com.lexho.user_service.payment.service.AdminWebhookQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/razorpay/webhooks", "/api/v1/admin/razorpay/webhooks"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminRazorpayWebhookController {

    private final AdminWebhookQueryService adminWebhookQueryService;

    public AdminRazorpayWebhookController(AdminWebhookQueryService adminWebhookQueryService) {
        this.adminWebhookQueryService = adminWebhookQueryService;
    }

    @GetMapping
    public DataResponse<AdminWebhookEventListResponse> list(@RequestParam(required = false) String search,
                                                            @RequestParam(required = false) String eventType,
                                                            @RequestParam(required = false) String status) {
        return adminWebhookQueryService.list(search, eventType, status);
    }

    @GetMapping("/{id}")
    public DataResponse<AdminWebhookEventResponse> detail(@PathVariable Long id) {
        return adminWebhookQueryService.detail(id);
    }
}
