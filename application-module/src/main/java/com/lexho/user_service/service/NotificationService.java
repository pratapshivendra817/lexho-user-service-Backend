package com.lexho.user_service.service;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void send(Long userId, String title, String message, String type, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(NotificationUserType.CUSTOMER);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type == null ? NotificationType.SYSTEM : NotificationType.valueOf(type.toUpperCase()));
        notification.setChannelType(ChannelType.IN_APP);
        notification.setPriority(NotificationPriority.MEDIUM);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(referenceId);
        notification.setSentAt(LocalDateTime.now());
        notification.setDeleted(false);
        notification.setRetryCount(0);
        notificationRepository.save(notification);
    }

    public List<Notification> getAll(Long userId) {
        return notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnread(Long userId) {
        return notificationRepository.findByUserIdAndStatusNotAndDeletedFalseOrderByCreatedAtDesc(userId, NotificationStatus.READ);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatusNotAndDeletedFalse(userId, NotificationStatus.READ);
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!userId.equals(notification.getUserId())) {
            throw new UnauthorizedException("You are not allowed to update this notification");
        }
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
