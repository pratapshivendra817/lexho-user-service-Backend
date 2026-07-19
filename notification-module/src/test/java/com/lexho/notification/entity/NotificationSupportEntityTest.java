package com.lexho.notification.entity;

import com.lexho.notification.enums.DeliveryStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.TemplateLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationSupportEntityTest {

    @Test
    void notificationLogPrePersistShouldSetCreatedAtAndDefaultRetryCount() {
        NotificationLog log = new NotificationLog();
        log.setNotificationId(10L);
        log.setDeliveryStatus(DeliveryStatus.FAILED);

        log.prePersist();

        assertNotNull(log.getCreatedAt());
        assertEquals(0, log.getRetryCount());
    }

    @Test
    void notificationPreferenceShouldKeepExpectedDefaults() {
        NotificationPreference preference = new NotificationPreference();

        assertTrue(preference.isPushEnabled());
        assertFalse(preference.isSmsEnabled());
        assertTrue(preference.isEmailEnabled());
        assertFalse(preference.isWhatsappEnabled());
        assertTrue(preference.isPromotionalEnabled());
        assertTrue(preference.isBookingEnabled());
    }

    @Test
    void notificationTemplatePrePersistShouldSetCreatedAt() {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateName("booking-created");
        template.setNotificationType(NotificationType.BOOKING);
        template.setLanguage(TemplateLanguage.ENGLISH);
        template.setTitleTemplate("Booking confirmed");
        template.setMessageTemplate("Your booking is confirmed");
        template.setActive(true);

        template.prePersist();

        assertNotNull(template.getCreatedAt());
    }
}
