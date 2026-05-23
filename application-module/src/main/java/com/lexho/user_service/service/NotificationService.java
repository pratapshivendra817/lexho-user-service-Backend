package com.lexho.user_service.service;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.notification.service.PushTokenService;
import com.lexho.user_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PushTokenService pushTokenService;

    public void send(Long userId, String title, String message, String type, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(NotificationUserType.CUSTOMER);
        notification.setTitle(title);
        notification.setMessage(message);
        NotificationType resolvedType = NotificationType.SYSTEM;
        if (type != null) {
            String normalized = type.trim().toUpperCase();
            if ("ORDER".equals(normalized)) {
                normalized = "BOOKING";
            }
            resolvedType = NotificationType.valueOf(normalized);
        }
        notification.setNotificationType(resolvedType);
        notification.setChannelType(ChannelType.IN_APP);
        notification.setPriority(NotificationPriority.MEDIUM);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(referenceId);
        notification.setSentAt(LocalDateTime.now());
        notification.setDeleted(false);
        notification.setRetryCount(0);
        notificationRepository.save(notification);
        // FCM-ready hook: currently logs token fanout; replace with Firebase send in next phase.
        for (String token : pushTokenService.activeTokens(userId, NotificationUserType.CUSTOMER.name())) {
            log.info("Push notification queued userId={} tokenPrefix={} type={}", userId, token.substring(0, Math.min(8, token.length())), type);
        }
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

    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!userId.equals(notification.getUserId())) {
            throw new UnauthorizedException("You are not allowed to delete this notification");
        }
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }
}
