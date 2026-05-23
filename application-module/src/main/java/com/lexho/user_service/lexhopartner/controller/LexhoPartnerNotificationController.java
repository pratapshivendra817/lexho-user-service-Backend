package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.service.NotificationManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/partner/notifications", "/api/v1/partner/notifications"})
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
@Tag(name = "Lexho Partner Notifications")
public class LexhoPartnerNotificationController {

    private final NotificationManagementService notificationManagementService;

    public LexhoPartnerNotificationController(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @GetMapping
    public ApiResponse<NotificationPageResponse<NotificationResponse>> notifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Notifications fetched successfully",
                notificationManagementService.getProviderNotifications(page, size));
    }
}
