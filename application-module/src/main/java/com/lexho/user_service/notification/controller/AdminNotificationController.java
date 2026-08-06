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
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Locale;

@RestController
@RequestMapping({"/api/admin/notifications", "/api/v1/admin/notifications"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Notification APIs", description = "Admin notification management APIs")
public class AdminNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);

    private final NotificationManagementService notificationManagementService;

    @Autowired(required = false)
    private NotificationRealtimeService notificationRealtimeService;

    public AdminNotificationController(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send bulk notifications")
    public ResponseEntity<NotificationSendResponse> send(@Valid @RequestBody AdminBulkNotificationRequest request) {
        return ResponseEntity.ok(notificationManagementService.sendBulkNotification(request));
    }

    @PostMapping
    @Operation(summary = "Send bulk notifications (legacy route)")
    public ResponseEntity<NotificationSendResponse> sendLegacy(@Valid @RequestBody AdminBulkNotificationRequest request) {
        return send(request);
    }

    @GetMapping
    @Operation(summary = "Get all notifications with filters")
    public ResponseEntity<NotificationPageResponse<NotificationResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) ChannelType channelType,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(notificationManagementService.getAdminNotifications(
                page, size, sortBy, sortDir, normalizeUserType(userType),
                normalizeNotificationType(notificationType), channelType, status, search
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
        try {
            return ResponseEntity.ok(notificationManagementService.retryNotification(id));
        } catch (NotificationValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Admin retry API failed. notificationId={}", id, ex);
            throw new NotificationValidationException("Retry failed: " + ex.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification dashboard stats")
    public ResponseEntity<NotificationStatsResponse> stats() {
        return ResponseEntity.ok(notificationManagementService.getStats());
    }

    @GetMapping("/stream")
    @Operation(summary = "Stream admin notifications in real time")
    public SseEmitter stream() {
        if (notificationRealtimeService == null) {
            throw new NotificationValidationException("Admin notification realtime stream is not available");
        }
        return notificationRealtimeService.subscribeAdmin();
    }

    private NotificationUserType normalizeUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return null;
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CUSTOMER", "CUSTOMERS" -> NotificationUserType.CUSTOMER;
            case "PARTNER", "PARTNERS" -> NotificationUserType.PARTNER;
            case "ADMIN", "ADMINS" -> NotificationUserType.ADMIN;
            default -> throw new IllegalArgumentException("Invalid userType");
        };
    }

    private NotificationType normalizeNotificationType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return null;
        }
        String normalized = notificationType.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "OFFER", "OFFERS", "PROMOTIONAL_OFFERS" -> "PROMOTIONAL_OFFER";
            case "PAYMENT_SUCCESS" -> "PAYMENT_SUCCESSFUL";
            default -> normalized;
        };
        try {
            return NotificationType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid notificationType");
        }
    }
}
