package com.lexho.user_service.notification.service;

import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.notification.event.SystemNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificationTriggerService {

    private final ApplicationEventPublisher eventPublisher;

    public NotificationTriggerService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void bookingCreated(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.BOOKING, "Booking Created",
                "Your booking " + bookingId + " has been created.", bookingId);
    }

    public void bookingConfirmed(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.BOOKING_CONFIRMED, "Booking Confirmed",
                "Your booking " + bookingId + " is confirmed.", bookingId);
    }

    public void providerAssigned(Long userId, String providerName, String bookingId) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ASSIGNED, "Partner Assigned",
                providerName + " has been assigned for booking " + bookingId + ".", bookingId);
    }

    public void professionalOnTheWay(Long userId, String providerName, String bookingId) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ON_THE_WAY, providerName + " is on the way",
                "Your professional is on the way for booking " + bookingId + ".", bookingId);
    }

    public void serviceStarted(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.SERVICE_STARTED, "Service Started",
                "Your service for booking " + bookingId + " has started.", bookingId);
    }

    public void serviceCompleted(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.SERVICE_COMPLETED, "Service Completed",
                "Please review your booking " + bookingId + ".", bookingId);
    }

    public void paymentSuccess(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.PAYMENT_SUCCESSFUL, "Payment Successful",
                "Payment for booking " + bookingId + " completed successfully.", bookingId);
    }

    public void refundInitiated(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.PAYMENT, "Refund Initiated",
                "Refund for booking " + bookingId + " is initiated.", bookingId);
    }

    public void subscriptionReminder(Long userId, String subscriptionId, String message) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.SUBSCRIPTION_REMINDER, NotificationPriority.MEDIUM,
                "Subscription Reminder", message, parseReferenceId(subscriptionId), "SUBSCRIPTION");
    }

    public void promotionalOffer(Long userId, String title, String message) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.PROMOTIONAL_OFFER, NotificationPriority.MEDIUM,
                title, message, null, "PROMOTION");
    }

    public void emergencyAlert(Long adminId, String title, String message, Long referenceId) {
        eventPublisher.publishEvent(new SystemNotificationEvent(
                adminId,
                NotificationUserType.ADMIN,
                NotificationType.SYSTEM,
                NotificationPriority.HIGH,
                title,
                message,
                referenceId,
                "EMERGENCY_EVENT",
                "EMERGENCY_SOS",
                referenceId == null ? null : String.valueOf(referenceId)
        ));
    }

    private void publish(Long userId,
                         NotificationUserType userType,
                         NotificationType type,
                         NotificationPriority priority,
                         String title,
                         String message,
                         Long referenceId,
                         String referenceType) {
        eventPublisher.publishEvent(new SystemNotificationEvent(
                userId,
                userType,
                type,
                priority,
                title,
                message,
                referenceId,
                referenceType
        ));
    }

    private void publishBooking(Long userId, NotificationType type, String title, String message, String bookingId) {
        eventPublisher.publishEvent(new SystemNotificationEvent(
                userId,
                NotificationUserType.CUSTOMER,
                type,
                NotificationPriority.HIGH,
                title,
                message,
                parseReferenceId(bookingId),
                "BOOKING",
                "BOOKING",
                bookingId
        ));
    }

    private Long parseReferenceId(String value) {
        if (value == null || value.isBlank() || !value.trim().chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.parseLong(value.trim());
    }
}
