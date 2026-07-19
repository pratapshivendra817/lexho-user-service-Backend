package com.lexho.notification.entity;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_id", columnList = "user_id"),
                @Index(name = "idx_notifications_status", columnList = "status"),
                @Index(name = "idx_notifications_type", columnList = "notification_type"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private NotificationUserType userType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "redirect_type", length = 60)
    private String redirectType;

    @Column(name = "redirect_value", length = 500)
    private String redirectValue;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (userType == null) {
            userType = NotificationUserType.CUSTOMER;
        }
        if (notificationType == null) {
            notificationType = NotificationType.SYSTEM;
        }
        if (channelType == null) {
            channelType = ChannelType.IN_APP;
        }
        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }
        if (status == null) {
            status = NotificationStatus.QUEUED;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    // Backward compatibility for existing code paths.
    public Boolean getIsRead() {
        return status == NotificationStatus.READ;
    }

    public void setIsRead(Boolean read) {
        if (Boolean.TRUE.equals(read)) {
            status = NotificationStatus.READ;
            readAt = LocalDateTime.now();
        } else if (status == NotificationStatus.READ) {
            status = NotificationStatus.SENT;
            readAt = null;
        }
    }

    public String getType() {
        return notificationType == null ? null : notificationType.name();
    }

    public void setType(String type) {
        if (type == null || type.isBlank()) {
            notificationType = NotificationType.SYSTEM;
            return;
        }
        try {
            notificationType = NotificationType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            notificationType = NotificationType.SYSTEM;
        }
    }
}
