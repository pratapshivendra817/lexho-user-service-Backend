package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.notification.service.EmailNotificationService;
import com.lexho.user_service.notification.service.FcmPushService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import com.lexho.user_service.notification.service.PushTokenService;
import com.lexho.user_service.notification.service.SmsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    private final NotificationManagementService notificationManagementService;
    private final PushTokenService pushTokenService;
    private final FcmPushService fcmPushService;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDispatchListener(NotificationManagementService notificationManagementService,
                                        PushTokenService pushTokenService,
                                        FcmPushService fcmPushService,
                                        EmailNotificationService emailNotificationService,
                                        SmsNotificationService smsNotificationService,
                                        NotificationRealtimeService notificationRealtimeService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.notificationManagementService = notificationManagementService;
        this.pushTokenService = pushTokenService;
        this.fcmPushService = fcmPushService;
        this.emailNotificationService = emailNotificationService;
        this.smsNotificationService = smsNotificationService;
        this.notificationRealtimeService = notificationRealtimeService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(NotificationDispatchEvent event) {
        try {
            Notification notification = notificationManagementService.findActive(event.getNotificationId());
            dispatchByChannel(notification, event.getChannelType());
            notificationManagementService.markDispatched(notification);
        } catch (NonRetryableNotificationDispatchException exception) {
            Notification notification = null;
            try {
                notification = notificationManagementService.findActive(event.getNotificationId());
            } catch (Exception ignored) {
                // If notification is still unavailable, there is nothing to update.
            }
            log.warn("Notification dispatch skipped. notificationId={}, channel={}, reason={}",
                    event.getNotificationId(), event.getChannelType(), exception.getMessage());
            if (notification != null) {
                notificationManagementService.markFailedPermanently(notification, exception.getMessage());
            }
        } catch (Exception exception) {
            Notification notification = null;
            try {
                notification = notificationManagementService.findActive(event.getNotificationId());
            } catch (Exception ignored) {
                // If notification is still unavailable, there is nothing to update.
            }
            log.error("Notification dispatch failed. notificationId={}, channel={}",
                    event.getNotificationId(), event.getChannelType(), exception);
            if (notification != null) {
                notificationManagementService.markFailed(notification, exception.getMessage());
            }
        }
    }

    private void dispatchByChannel(Notification notification, ChannelType channelType) {
        switch (channelType) {
            case IN_APP -> dispatchInApp(notification);
            case PUSH -> dispatchPush(notification);
            case EMAIL -> dispatchEmail(notification);
            case SMS -> dispatchSms(notification);
            case WHATSAPP -> dispatchWhatsApp(notification);
        }
    }

    private void dispatchInApp(Notification notification) {
        Map<String, Object> payload = Map.of(
                "id", notification.getId(),
                "title", notification.getTitle(),
                "message", notification.getMessage(),
                "type", notification.getNotificationType().name(),
                "status", NotificationStatus.SENT.name(),
                "userId", notification.getUserId(),
                "userType", notification.getUserType() == null ? "" : notification.getUserType().name()
        );
        messagingTemplate.convertAndSend("/topic/notifications/" + notification.getUserId(), payload);
        messagingTemplate.convertAndSend("/topic/admin/notifications", payload);
        notificationRealtimeService.publish(notification.getUserId(), "notification-created", payload);
        notificationRealtimeService.publishAdmin("notification-created", payload);
    }

    private void dispatchPush(Notification notification) {
        String role = resolveRole(notification.getUserType());
        List<String> tokens = pushTokenService.activeTokens(notification.getUserId(), role);
        if (tokens.isEmpty()) {
            throw new NonRetryableNotificationDispatchException("No active device token found for push notification");
        }
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getNotificationType().name());
        data.put("notificationType", notification.getNotificationType().name());
        data.put("userType", notification.getUserType() == null ? "" : notification.getUserType().name());
        data.put("referenceType", notification.getReferenceType() == null ? "" : notification.getReferenceType());
        data.put("referenceId", notification.getReferenceId() == null ? "" : String.valueOf(notification.getReferenceId()));
        data.put("redirectType", notification.getRedirectType() == null ? "" : notification.getRedirectType());
        data.put("redirectValue", notification.getRedirectValue() == null ? "" : notification.getRedirectValue());
        if ("WARNING".equalsIgnoreCase(notification.getRedirectType())) {
            String warningId = notification.getRedirectValue() == null ? "" : notification.getRedirectValue();
            data.put("warningId", warningId);
            data.put("title", notification.getTitle());
            data.put("message", notification.getMessage());
        }
        int sent = fcmPushService.sendToTokens(tokens, notification.getTitle(), notification.getMessage(), data);
        if (sent <= 0) {
            throw new IllegalStateException("FCM dispatch failed for all tokens without a Firebase error response");
        }
        log.info("Push dispatched for userId={} sentCount={} tokenCount={}",
                notification.getUserId(), sent, tokens.size());
    }

    private void dispatchEmail(Notification notification) {
        emailNotificationService.send(notification);
        log.info("Email dispatched for notificationId={}", notification.getId());
    }

    private void dispatchSms(Notification notification) {
        smsNotificationService.send(notification);
        log.info("SMS dispatched for notificationId={}", notification.getId());
    }

    private void dispatchWhatsApp(Notification notification) {
        // WhatsApp partner integration hook point.
        log.info("WhatsApp queued for notificationId={}", notification.getId());
    }

    private String resolveRole(NotificationUserType userType) {
        if (userType == null) {
            return "CUSTOMER";
        }
        return userType.name().toUpperCase(Locale.ROOT);
    }

    private static class NonRetryableNotificationDispatchException extends RuntimeException {
        private NonRetryableNotificationDispatchException(String message) {
            super(message);
        }
    }
}
