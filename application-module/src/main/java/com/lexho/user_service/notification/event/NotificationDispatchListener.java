package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.entity.UserDeviceToken;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.user_service.notification.service.NotificationManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    private final NotificationManagementService notificationManagementService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDispatchListener(NotificationManagementService notificationManagementService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.notificationManagementService = notificationManagementService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @EventListener
    public void onDispatch(NotificationDispatchEvent event) {
        Notification notification = notificationManagementService.findActive(event.getNotificationId());
        try {
            dispatchByChannel(notification, event.getChannelType());
            notificationManagementService.markDispatched(notification);
        } catch (Exception exception) {
            log.error("Notification dispatch failed. notificationId={}, channel={}",
                    notification.getId(), event.getChannelType(), exception);
            notificationManagementService.markFailed(notification, exception.getMessage());
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
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getUserId(),
                Map.of(
                        "id", notification.getId(),
                        "title", notification.getTitle(),
                        "message", notification.getMessage(),
                        "type", notification.getNotificationType().name(),
                        "status", NotificationStatus.SENT.name()
                )
        );
    }

    private void dispatchPush(Notification notification) {
        List<UserDeviceToken> deviceTokens = notificationManagementService.activeDeviceTokens(notification.getUserId());
        if (deviceTokens.isEmpty()) {
            throw new IllegalStateException("No active device token found for push notification");
        }
        // FCM integration hook point.
        log.info("Push queued for userId={} tokenCount={}", notification.getUserId(), deviceTokens.size());
    }

    private void dispatchEmail(Notification notification) {
        // SMTP/ESP integration hook point.
        log.info("Email queued for notificationId={}", notification.getId());
    }

    private void dispatchSms(Notification notification) {
        // SMS provider integration hook point.
        log.info("SMS queued for notificationId={}", notification.getId());
    }

    private void dispatchWhatsApp(Notification notification) {
        // WhatsApp provider integration hook point.
        log.info("WhatsApp queued for notificationId={}", notification.getId());
    }
}
