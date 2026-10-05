package com.lexho.notification.entity;

import com.lexho.notification.enums.DeliveryStatus;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.enums.TemplateLanguage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void notificationLogAccessorsAndExistingRetryCountArePreserved() {
        NotificationLog log = new NotificationLog();
        log.setId(1L);
        log.setNotificationId(10L);
        log.setDeliveryStatus(DeliveryStatus.SENT);
        log.setErrorMessage("none");
        log.setProviderMessageId("provider-1");
        log.setFailureCode("N/A");
        log.setRetryCount(2);

        log.prePersist();

        assertEquals(1L, log.getId());
        assertEquals(10L, log.getNotificationId());
        assertEquals(DeliveryStatus.SENT, log.getDeliveryStatus());
        assertEquals("none", log.getErrorMessage());
        assertEquals("provider-1", log.getProviderMessageId());
        assertEquals("N/A", log.getFailureCode());
        assertEquals(2, log.getRetryCount());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void notificationPreferenceAccessorsRoundTripValues() {
        NotificationPreference preference = new NotificationPreference();
        preference.setId(1L);
        preference.setUserId(42L);
        preference.setPushEnabled(false);
        preference.setSmsEnabled(true);
        preference.setEmailEnabled(false);
        preference.setWhatsappEnabled(true);
        preference.setPromotionalEnabled(false);
        preference.setBookingEnabled(false);

        assertEquals(1L, preference.getId());
        assertEquals(42L, preference.getUserId());
        assertFalse(preference.isPushEnabled());
        assertTrue(preference.isSmsEnabled());
        assertFalse(preference.isEmailEnabled());
        assertTrue(preference.isWhatsappEnabled());
        assertFalse(preference.isPromotionalEnabled());
        assertFalse(preference.isBookingEnabled());
    }

    @Test
    void notificationTemplateAccessorsRoundTripValues() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 27, 10, 0);
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setTemplateName("booking-created");
        template.setNotificationType(NotificationType.BOOKING);
        template.setTitleTemplate("Title");
        template.setMessageTemplate("Message");
        template.setLanguage(TemplateLanguage.HINDI);
        template.setActive(true);
        template.setCreatedAt(createdAt);

        assertEquals(1L, template.getId());
        assertEquals("booking-created", template.getTemplateName());
        assertEquals(NotificationType.BOOKING, template.getNotificationType());
        assertEquals("Title", template.getTitleTemplate());
        assertEquals("Message", template.getMessageTemplate());
        assertEquals(TemplateLanguage.HINDI, template.getLanguage());
        assertTrue(template.isActive());
        assertEquals(createdAt, template.getCreatedAt());
    }

    @Test
    void notificationPreUpdateDefaultsRetryCountAndReadCompatibilityCanClearStatus() {
        Notification notification = new Notification();
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.of(2026, 8, 27, 10, 0));
        notification.setRetryCount(null);

        notification.setIsRead(false);
        notification.preUpdate();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNull(notification.getReadAt());
        assertEquals(0, notification.getRetryCount());
        assertNotNull(notification.getUpdatedAt());
    }

    @Test
    void notificationAccessorsAndDefaultsRoundTripValues() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 27, 10, 0);
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(42L);
        notification.setUserType(NotificationUserType.PARTNER);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setNotificationType(NotificationType.PAYMENT);
        notification.setChannelType(com.lexho.notification.enums.ChannelType.EMAIL);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(100L);
        notification.setReferenceType("BOOKING");
        notification.setRedirectType("DEEPLINK");
        notification.setRedirectValue("app://booking/100");
        notification.setIdempotencyKey("idem-1");
        notification.setScheduledTime(now);
        notification.setExpiryTime(now.plusDays(1));
        notification.setSentAt(now.plusMinutes(1));
        notification.setDeliveredAt(now.plusMinutes(2));
        notification.setClickedAt(now.plusMinutes(3));
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setDeleted(true);

        assertEquals(1L, notification.getId());
        assertEquals(42L, notification.getUserId());
        assertEquals(NotificationUserType.PARTNER, notification.getUserType());
        assertEquals("Title", notification.getTitle());
        assertEquals("Message", notification.getMessage());
        assertEquals(NotificationType.PAYMENT, notification.getNotificationType());
        assertEquals(NotificationPriority.HIGH, notification.getPriority());
        assertEquals(100L, notification.getReferenceId());
        assertEquals("BOOKING", notification.getReferenceType());
        assertEquals("DEEPLINK", notification.getRedirectType());
        assertEquals("app://booking/100", notification.getRedirectValue());
        assertEquals("idem-1", notification.getIdempotencyKey());
        assertEquals(now, notification.getScheduledTime());
        assertEquals(now.plusDays(1), notification.getExpiryTime());
        assertEquals(now.plusMinutes(1), notification.getSentAt());
        assertEquals(now.plusMinutes(2), notification.getDeliveredAt());
        assertEquals(now.plusMinutes(3), notification.getClickedAt());
        assertEquals(now, notification.getCreatedAt());
        assertEquals(now, notification.getUpdatedAt());
        assertTrue(notification.isDeleted());
    }
}
