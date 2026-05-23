package com.lexho.user_service.notification.dto;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationUserType userType;
    private String title;
    private String message;
    private NotificationType notificationType;
    private ChannelType channelType;
    private NotificationPriority priority;
    private NotificationStatus status;
    private Long referenceId;
    private String referenceType;
    private String redirectType;
    private String bookingId;
    private String redirectValue;
    private LocalDateTime scheduledTime;
    private LocalDateTime expiryTime;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime clickedAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
