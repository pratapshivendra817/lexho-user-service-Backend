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
    @JsonAlias({"userType", "audience", "appType"})
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
        addChannel(channelType);
    }

    @JsonSetter("userId")
    public void setUserId(Long userId) {
        addUserId(userId);
    }

    @JsonSetter("partnerId")
    public void setPartnerId(Long partnerId) {
        addUserId(partnerId);
    }

    @JsonSetter("providerId")
    public void setProviderId(Long providerId) {
        addUserId(providerId);
    }

    @JsonSetter("audience")
    public void setAudience(String audience) {
        this.targetUserType = parseUserType(audience);
    }

    @JsonSetter("appType")
    public void setAppType(String appType) {
        this.targetUserType = parseUserType(appType);
    }

    private void addUserId(Long userId) {
        if (userId == null) {
            return;
        }
        if (this.userIds == null) {
            this.userIds = new ArrayList<>();
        }
        if (!this.userIds.contains(userId)) {
            this.userIds.add(userId);
        }
    }

    private void addChannel(ChannelType channelType) {
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
        this.notificationType = parseNotificationType(notificationType);
    }

    @JsonSetter("type")
    public void setLegacyType(String type) {
        this.notificationType = parseNotificationType(type);
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

    private NotificationType parseNotificationType(String value) {
        if (value == null || value.isBlank()) {
            return this.notificationType;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "OFFER", "OFFERS", "PROMOTIONAL_OFFERS" -> "PROMOTIONAL_OFFER";
            case "PAYMENT_SUCCESS" -> "PAYMENT_SUCCESSFUL";
            case "SUBSCRIPTION_PURCHASED" -> "REMINDER";
            case "ONBOARDING", "PARTNER_ONBOARDING", "ONBOARDING_STATUS" -> "SYSTEM";
            case "PUSH" -> {
                addChannel(ChannelType.IN_APP);
                addChannel(ChannelType.PUSH);
                yield "SYSTEM";
            }
            case "IN_APP" -> {
                addChannel(ChannelType.IN_APP);
                yield "SYSTEM";
            }
            case "EMAIL" -> {
                addChannel(ChannelType.EMAIL);
                yield "SYSTEM";
            }
            case "SMS" -> {
                addChannel(ChannelType.SMS);
                yield "SYSTEM";
            }
            case "WHATSAPP" -> {
                addChannel(ChannelType.WHATSAPP);
                yield "SYSTEM";
            }
            default -> normalized;
        };
        return NotificationType.valueOf(normalized);
    }
}
