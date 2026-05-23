package com.lexho.user_service.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class AdminBulkNotificationRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must be at most 2000 characters")
    private String message;

    @NotNull(message = "notificationType is required")
    @JsonAlias({"type"})
    private NotificationType notificationType;

    private NotificationPriority priority;

    @NotEmpty(message = "channels are required")
    @JsonAlias({"channelTypes"})
    private List<ChannelType> channels;

    @NotNull(message = "targetUserType is required")
    @JsonAlias({"userType"})
    private NotificationUserType targetUserType;

    private List<Long> userIds;
    private String city;
    private String serviceTarget;
    private Boolean inactiveUsers;
    private Boolean premiumUsers;

    private Long referenceId;
    private String referenceType;
    private String redirectType;
    private String redirectValue;
    private LocalDateTime scheduledTime;
    private LocalDateTime expiryTime;
    private Map<String, String> templateVariables;

    @JsonSetter("channelType")
    public void setChannelType(ChannelType channelType) {
        if (channelType == null) {
            return;
        }
        if (this.channels == null) {
            this.channels = new ArrayList<>();
        }
        if (!this.channels.contains(channelType)) {
            this.channels.add(channelType);
        }
    }

    @JsonSetter("userType")
    public void setLegacyUserType(String userType) {
        this.targetUserType = parseUserType(userType);
    }

    @JsonSetter("targetUserType")
    public void setTargetUserTypeFromString(String targetUserType) {
        this.targetUserType = parseUserType(targetUserType);
    }

    @JsonSetter("notificationType")
    public void setNotificationTypeFromString(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return;
        }
        String normalized = notificationType.trim().toUpperCase(Locale.ROOT);
        if ("OFFER".equals(normalized)) {
            normalized = "PROMOTION";
        }
        this.notificationType = NotificationType.valueOf(normalized);
    }

    private NotificationUserType parseUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return this.targetUserType;
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        if ("CUSTOMERS".equals(normalized)) {
            normalized = "CUSTOMER";
        } else if ("PARTNERS".equals(normalized)) {
            normalized = "PARTNER";
        } else if ("ADMINS".equals(normalized)) {
            normalized = "ADMIN";
        }
        return NotificationUserType.valueOf(normalized);
    }
}
