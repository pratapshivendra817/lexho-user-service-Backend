package com.lexho.user_service.notification.controller;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.NotificationSendResponse;
import com.lexho.user_service.notification.dto.NotificationStatsResponse;
import com.lexho.user_service.notification.service.NotificationManagementService;
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

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin Notification APIs", description = "Admin notification management APIs")
public class AdminNotificationController {

    private final NotificationManagementService notificationManagementService;

    public AdminNotificationController(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send bulk notifications")
    public ResponseEntity<NotificationSendResponse> send(@Valid @RequestBody AdminBulkNotificationRequest request) {
        return ResponseEntity.ok(notificationManagementService.sendBulkNotification(request));
    }

    @GetMapping
    @Operation(summary = "Get all notifications with filters")
    public ResponseEntity<NotificationPageResponse<NotificationResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) NotificationUserType userType,
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) ChannelType channelType,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(notificationManagementService.getAdminNotifications(
                page, size, sortBy, sortDir, userType, notificationType, channelType, status, search
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by id")
    public ResponseEntity<NotificationResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(notificationManagementService.getAdminNotificationById(id));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed notification")
    public ResponseEntity<NotificationResponse> retry(@PathVariable Long id) {
        return ResponseEntity.ok(notificationManagementService.retryNotification(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification dashboard stats")
    public ResponseEntity<NotificationStatsResponse> stats() {
        return ResponseEntity.ok(notificationManagementService.getStats());
    }
}
