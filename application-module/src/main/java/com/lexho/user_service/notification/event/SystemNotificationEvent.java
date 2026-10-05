package com.lexho.user_service.notification.event;

import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import lombok.Getter;

@Getter
public class SystemNotificationEvent {

    private final Long userId;
    private final NotificationUserType userType;
    private final NotificationType notificationType;
    private final NotificationPriority priority;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String referenceType;
    private final String redirectType;
    private final String redirectValue;
    private final String idempotencyKeyPrefix;

    public SystemNotificationEvent(Long userId,
                                   NotificationUserType userType,
                                   NotificationType notificationType,
                                   NotificationPriority priority,
                                   String title,
                                   String message,
                                   Long referenceId,
                                   String referenceType) {
        this(userId, userType, notificationType, priority, title, message, referenceId, referenceType,
                referenceType, referenceId == null ? null : String.valueOf(referenceId), null);
    }

    public SystemNotificationEvent(Long userId,
                                   NotificationUserType userType,
                                   NotificationType notificationType,
                                   NotificationPriority priority,
                                   String title,
                                   String message,
                                   Long referenceId,
                                   String referenceType,
                                   String redirectType,
                                   String redirectValue) {
        this(userId, userType, notificationType, priority, title, message, referenceId, referenceType,
                redirectType, redirectValue, null);
    }

    public SystemNotificationEvent(Long userId,
                                   NotificationUserType userType,
                                   NotificationType notificationType,
                                   NotificationPriority priority,
                                   String title,
                                   String message,
                                   Long referenceId,
                                   String referenceType,
                                   String redirectType,
                                   String redirectValue,
                                   String idempotencyKeyPrefix) {
        this.userId = userId;
        this.userType = userType;
        this.notificationType = notificationType;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.redirectType = redirectType;
        this.redirectValue = redirectValue;
        this.idempotencyKeyPrefix = idempotencyKeyPrefix;
    }
}
