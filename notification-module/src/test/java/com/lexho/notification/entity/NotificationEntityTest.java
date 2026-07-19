package com.lexho.notification.entity;

import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEntityTest {

    @Test
    void prePersistShouldApplyProductionDefaults() {
        Notification notification = new Notification();

        notification.prePersist();

        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
        assertEquals(NotificationUserType.CUSTOMER, notification.getUserType());
        assertEquals(NotificationType.SYSTEM, notification.getNotificationType());
        assertEquals(ChannelType.IN_APP, notification.getChannelType());
        assertEquals(NotificationPriority.MEDIUM, notification.getPriority());
        assertEquals(NotificationStatus.QUEUED, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
    }

    @Test
    void readCompatibilityAccessorsShouldUpdateStatusAndReadAt() {
        Notification notification = new Notification();
        notification.setStatus(NotificationStatus.SENT);

        notification.setIsRead(true);

        assertTrue(notification.getIsRead());
        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());

        notification.setIsRead(false);

        assertFalse(notification.getIsRead());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNull(notification.getReadAt());
    }

    @Test
    void typeCompatibilitySetterShouldParseCaseInsensitiveAndFallbackToSystem() {
        Notification notification = new Notification();

        notification.setType(" reminder ");

        assertEquals(NotificationType.REMINDER, notification.getNotificationType());
        assertEquals("REMINDER", notification.getType());

        notification.setType("unknown-type");

        assertEquals(NotificationType.SYSTEM, notification.getNotificationType());

        notification.setType(" ");

        assertEquals(NotificationType.SYSTEM, notification.getNotificationType());
    }

    @Test
    void preUpdateShouldRefreshUpdatedAtAndNormalizeRetryCount() {
        Notification notification = new Notification();
        notification.setRetryCount(null);

        notification.preUpdate();

        assertNotNull(notification.getUpdatedAt());
        assertEquals(0, notification.getRetryCount());
    }
}
