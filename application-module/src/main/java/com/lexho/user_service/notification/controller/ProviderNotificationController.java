package com.lexho.user_service.notification.controller;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.UnreadCountResponse;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.service.NotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/provider/notifications")
@PreAuthorize("hasRole('PARTNER')")
@Tag(name = "Partner Notification APIs", description = "Notification APIs for partner/partner app")
public class ProviderNotificationController {

    private final NotificationManagementService notificationManagementService;
    private final NotificationMapper notificationMapper;

    public ProviderNotificationController(NotificationManagementService notificationManagementService,
                                          NotificationMapper notificationMapper) {
        this.notificationManagementService = notificationManagementService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    @Operation(summary = "Get partner notifications")
    public ResponseEntity<NotificationPageResponse<NotificationResponse>> notifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationManagementService.getProviderNotifications(page, size));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark partner notification as read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        Notification updated = notificationManagementService.markAsReadForCurrentUser(id);
        return ResponseEntity.ok(notificationMapper.toResponse(updated));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get partner unread count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificationManagementService.unreadCountForCurrentUser()));
    }
}
