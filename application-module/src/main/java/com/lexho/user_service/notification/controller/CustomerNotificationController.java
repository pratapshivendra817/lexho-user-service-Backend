package com.lexho.user_service.notification.controller;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.dto.NotificationMessageResponse;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.UnreadCountResponse;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.service.NotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/notifications")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Notification APIs", description = "Notification APIs for customer app")
public class CustomerNotificationController {

    private final NotificationManagementService notificationManagementService;
    private final NotificationMapper notificationMapper;

    public CustomerNotificationController(NotificationManagementService notificationManagementService,
                                          NotificationMapper notificationMapper) {
        this.notificationManagementService = notificationManagementService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    @Operation(summary = "Get customer notifications")
    public ResponseEntity<NotificationPageResponse<NotificationResponse>> notifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationManagementService.getCustomerNotifications(page, size));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        Notification updated = notificationManagementService.markAsReadForCurrentUser(id);
        return ResponseEntity.ok(notificationMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete notification")
    public ResponseEntity<NotificationMessageResponse> delete(@PathVariable Long id) {
        notificationManagementService.softDeleteForCurrentUser(id);
        return ResponseEntity.ok(new NotificationMessageResponse("Notification deleted successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificationManagementService.unreadCountForCurrentUser()));
    }
}
