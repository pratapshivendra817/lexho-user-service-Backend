package com.lexho.user_service.notification.controller;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.dto.NotificationSendResponse;
import com.lexho.user_service.notification.dto.PushSendRequest;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
public class NotificationSendController {

    private static final Set<String> SUPPORTED_REDIRECT_TYPES = Set.of(
            "NOTIFICATIONS",
            "PAYMENT",
            "PAYMENTSTATUS",
            "INSTANT_BOOKING",
            "SEARCHING_PARTNER",
            "SCHEDULED_BOOKING",
            "TRACKING"
    );

    private final NotificationManagementService notificationManagementService;
    private final SecurityUtils securityUtils;

    public NotificationSendController(NotificationManagementService notificationManagementService,
                                      SecurityUtils securityUtils) {
        this.notificationManagementService = notificationManagementService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','PARTNER')")
    public ApiResponse<NotificationSendResponse> send(@Valid @RequestBody PushSendRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId().orElse(null);
        String currentRole = securityUtils.getCurrentRole().orElse("");
        if (!"ADMIN".equalsIgnoreCase(currentRole) && currentUserId != null && !currentUserId.equals(request.getUserId())) {
            throw new NotificationAccessDeniedException("You can only send test notifications for your own user");
        }

        String redirectType = normalizeRedirectType(request);
        AdminBulkNotificationRequest bulk = new AdminBulkNotificationRequest();
        bulk.setTitle(request.getTitle());
        bulk.setMessage(request.getMessage());
        bulk.setNotificationType(NotificationType.SYSTEM);
        bulk.setChannels(List.of(ChannelType.PUSH, ChannelType.IN_APP));
        bulk.setTargetUserType(NotificationUserType.CUSTOMER);
        bulk.setUserIds(List.of(request.getUserId()));
        bulk.setRedirectType(redirectType);

        String bookingId = request.getData() == null ? null : request.getData().getBookingId();
        if (bookingId != null && !bookingId.isBlank()) {
            bulk.setReferenceType("BOOKING");
            bulk.setRedirectValue(bookingId.trim());
            if (bookingId.trim().chars().allMatch(Character::isDigit)) {
                bulk.setReferenceId(Long.parseLong(bookingId.trim()));
            }
        }

        NotificationSendResponse response = notificationManagementService.sendBulkNotification(bulk);
        return ApiResponse.success("Notification sent successfully", response);
    }

    private String normalizeRedirectType(PushSendRequest request) {
        if (request.getData() == null || request.getData().getRedirectType() == null) {
            throw new NotificationValidationException("data.redirectType is required");
        }
        String redirectType = request.getData().getRedirectType().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_REDIRECT_TYPES.contains(redirectType)) {
            throw new NotificationValidationException("Unsupported redirectType");
        }
        return redirectType;
    }
}
