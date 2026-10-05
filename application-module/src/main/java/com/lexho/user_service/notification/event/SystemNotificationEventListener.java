package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class SystemNotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SystemNotificationEventListener(NotificationRepository notificationRepository,
                                           ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSystemEvent(SystemNotificationEvent event) {
        for (ChannelType channelType : List.of(ChannelType.IN_APP, ChannelType.PUSH)) {
            Notification notification = buildNotification(event, channelType);
            if (isDuplicate(notification)) {
                continue;
            }
            Notification saved;
            try {
                saved = notificationRepository.save(notification);
            } catch (DataIntegrityViolationException exception) {
                continue;
            }
            eventPublisher.publishEvent(new NotificationDispatchEvent(saved.getId(), saved.getChannelType()));
        }
    }

    private Notification buildNotification(SystemNotificationEvent event, ChannelType channelType) {
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setUserType(event.getUserType());
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setNotificationType(event.getNotificationType());
        notification.setPriority(event.getPriority());
        notification.setChannelType(channelType);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setReferenceId(event.getReferenceId());
        notification.setReferenceType(event.getReferenceType());
        notification.setRedirectType(event.getRedirectType());
        notification.setRedirectValue(event.getRedirectValue());
        notification.setRetryCount(0);
        notification.setDeleted(false);
        if (event.getIdempotencyKeyPrefix() != null && !event.getIdempotencyKeyPrefix().isBlank()) {
            notification.setIdempotencyKey(event.getIdempotencyKeyPrefix() + ":" + channelType.name());
        }
        return notification;
    }

    private boolean isDuplicate(Notification notification) {
        return notification.getIdempotencyKey() != null
                && notificationRepository.existsByIdempotencyKey(notification.getIdempotencyKey());
    }
}
