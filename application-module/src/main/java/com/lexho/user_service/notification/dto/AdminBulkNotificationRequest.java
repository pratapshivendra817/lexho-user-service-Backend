package com.lexho.user_service.notification.dto;

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
import java.util.List;
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
    private NotificationType notificationType;

    @NotNull(message = "priority is required")
    private NotificationPriority priority;

    @NotEmpty(message = "channels are required")
    private List<ChannelType> channels;

    @NotNull(message = "targetUserType is required")
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
}
