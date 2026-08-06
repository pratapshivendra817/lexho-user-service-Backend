package com.lexho.user_service.lexhopartner.controller;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.UnreadCountResponse;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping({
        "/api/partner/notifications",
        "/api/v1/partner/notifications",
        "/api/provider/notifications",
        "/api/v1/provider/notifications"
})
@PreAuthorize("hasRole('PARTNER')")
@Tag(name = "Lexho Partner Notifications")
public class LexhoPartnerNotificationController {

    private final NotificationManagementService notificationManagementService;
    private final NotificationMapper notificationMapper;
    private final NotificationRealtimeService notificationRealtimeService;

    public LexhoPartnerNotificationController(NotificationManagementService notificationManagementService,
                                              NotificationMapper notificationMapper,
                                              NotificationRealtimeService notificationRealtimeService) {
        this.notificationManagementService = notificationManagementService;
        this.notificationMapper = notificationMapper;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @GetMapping
    public ApiResponse<NotificationPageResponse<NotificationResponse>> notifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Notifications fetched successfully",
                notificationManagementService.getProviderNotifications(page, size));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        Notification updated = notificationManagementService.markAsReadForCurrentUser(id);
        notificationRealtimeService.publish(updated.getUserId(), "notification-read", Map.of(
                "id", updated.getId(),
                "status", updated.getStatus().name(),
                "readAt", updated.getReadAt()
        ));
        return ApiResponse.success("Notification marked as read", notificationMapper.toResponse(updated));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success("Unread count fetched successfully",
                new UnreadCountResponse(notificationManagementService.unreadCountForCurrentUser()));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestAttribute("userId") Long partnerId) {
        return notificationRealtimeService.subscribe(partnerId);
    }
}
