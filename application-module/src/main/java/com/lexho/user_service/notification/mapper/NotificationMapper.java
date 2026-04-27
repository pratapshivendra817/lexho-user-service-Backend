package com.lexho.user_service.notification.mapper;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setUserType(notification.getUserType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setNotificationType(notification.getNotificationType());
        response.setChannelType(notification.getChannelType());
        response.setPriority(notification.getPriority());
        response.setStatus(notification.getStatus());
        response.setReferenceId(notification.getReferenceId());
        response.setReferenceType(notification.getReferenceType());
        response.setRedirectType(notification.getRedirectType());
        response.setRedirectValue(notification.getRedirectValue());
        response.setScheduledTime(notification.getScheduledTime());
        response.setExpiryTime(notification.getExpiryTime());
        response.setSentAt(notification.getSentAt());
        response.setReadAt(notification.getReadAt());
        response.setClickedAt(notification.getClickedAt());
        response.setCreatedAt(notification.getCreatedAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        return response;
    }
}
