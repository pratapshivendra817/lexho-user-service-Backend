package com.lexho.user_service.notification.scheduler;

import com.lexho.user_service.notification.service.NotificationManagementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final NotificationManagementService notificationManagementService;

    public NotificationScheduler(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @Scheduled(fixedDelayString = "${app.notification.scheduler.scheduled-delay-ms:30000}")
    public void processScheduled() {
        notificationManagementService.processScheduledNotifications();
    }

    @Scheduled(fixedDelayString = "${app.notification.scheduler.retry-delay-ms:60000}")
    public void retryFailed() {
        notificationManagementService.retryFailedNotifications();
    }

    @Scheduled(cron = "${app.notification.scheduler.cleanup-cron:0 0 * * * *}")
    public void cleanupExpired() {
        notificationManagementService.cleanupExpiredNotifications();
    }
}
