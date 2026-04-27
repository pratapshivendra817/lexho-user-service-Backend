package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.repository.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SystemNotificationEventListener {

    private final NotificationRepository notificationRepository;

    public SystemNotificationEventListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @EventListener
    public void onSystemEvent(SystemNotificationEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setUserType(event.getUserType());
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setNotificationType(event.getNotificationType());
        notification.setPriority(event.getPriority());
        notification.setChannelType(ChannelType.IN_APP);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setReferenceId(event.getReferenceId());
        notification.setReferenceType(event.getReferenceType());
        notification.setRetryCount(0);
        notification.setDeleted(false);
        notificationRepository.save(notification);
    }
}
