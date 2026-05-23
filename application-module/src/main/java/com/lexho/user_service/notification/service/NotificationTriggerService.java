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
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.BOOKING, NotificationPriority.HIGH,
                "Booking Created", "Your booking " + bookingId + " has been created.", null, "BOOKING");
    }

    public void bookingConfirmed(Long userId, String bookingId) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.BOOKING, NotificationPriority.HIGH,
                "Booking Confirmed", "Your booking " + bookingId + " is confirmed.", null, "BOOKING");
    }

    public void providerAssigned(Long userId, String providerName, String bookingId) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.BOOKING, NotificationPriority.HIGH,
                "Partner Assigned", providerName + " has been assigned for booking " + bookingId + ".", null, "BOOKING");
    }

    public void serviceCompleted(Long userId, String bookingId) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.REVIEW, NotificationPriority.MEDIUM,
                "Service Completed", "Please review your booking " + bookingId + ".", null, "BOOKING");
    }

    public void paymentSuccess(Long userId, String bookingId) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.PAYMENT, NotificationPriority.HIGH,
                "Payment Successful", "Payment for booking " + bookingId + " completed successfully.", null, "BOOKING");
    }

    public void refundInitiated(Long userId, String bookingId) {
        publish(userId, NotificationUserType.CUSTOMER, NotificationType.PAYMENT, NotificationPriority.MEDIUM,
                "Refund Initiated", "Refund for booking " + bookingId + " is initiated.", null, "BOOKING");
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
}
