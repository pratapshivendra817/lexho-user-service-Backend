package com.lexho.user_service.notification.service;

import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.notification.event.SystemNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationTriggerService {
    private static final String REFERENCE_TYPE_BOOKING = "BOOKING";

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationTemplateService notificationTemplateService;

    public NotificationTriggerService(ApplicationEventPublisher eventPublisher) {
        this(eventPublisher, Optional.empty());
    }

    @Autowired
    public NotificationTriggerService(ApplicationEventPublisher eventPublisher,
                                      Optional<NotificationTemplateService> notificationTemplateService) {
        this.eventPublisher = eventPublisher;
        this.notificationTemplateService = optionalValue(notificationTemplateService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    public void bookingCreated(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.BOOKING, "Booking Created",
                "Your booking " + bookingId + " has been created.", bookingId);
    }

    public void bookingCreated(Long userId, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.BOOKING, "Booking Created",
                "Your booking " + bookingId + " has been created.", bookingId, redirectType, null);
    }

    public void bookingConfirmed(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.BOOKING_CONFIRMED, "Booking Confirmed",
                "Your booking " + bookingId + " is confirmed.", bookingId);
    }

    public void bookingConfirmed(Long userId, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.BOOKING_CONFIRMED, "Booking Confirmed",
                "Your booking " + bookingId + " is confirmed.", bookingId, redirectType, null);
    }

    public void providerAssigned(Long userId, String providerName, String bookingId) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ASSIGNED, "Partner Assigned",
                providerName + " has been assigned for booking " + bookingId + ".", bookingId);
    }

    public void providerAssigned(Long userId, String providerName, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ASSIGNED, "Partner Assigned",
                providerName + " has been assigned for booking " + bookingId + ".", bookingId, redirectType, null);
    }

    public void professionalOnTheWay(Long userId, String providerName, String bookingId) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ON_THE_WAY, providerName + " is on the way",
                "Your professional is on the way for booking " + bookingId + ".", bookingId);
    }

    public void professionalOnTheWay(Long userId, String providerName, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.PROFESSIONAL_ON_THE_WAY, providerName + " is on the way",
                "Your professional is on the way for booking " + bookingId + ".", bookingId, redirectType, null);
    }

    public void serviceStarted(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.SERVICE_STARTED, "Service Started",
                "Your service for booking " + bookingId + " has started.", bookingId);
    }

    public void serviceStarted(Long userId, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.SERVICE_STARTED, "Service Started",
                "Your service for booking " + bookingId + " has started.", bookingId, redirectType, null);
    }

    public void serviceCompleted(Long userId, String bookingId) {
        var template = render("SERVICE_COMPLETED_CUSTOMER", NotificationType.SERVICE_COMPLETED,
                "Service Completed", "Your service has been completed successfully.",
                Map.of("orderId", bookingId == null ? "" : bookingId));
        publishBooking(userId, template.notificationType(), template.title(), template.message(), bookingId,
                "order-completed:customer:" + bookingId);
    }

    public void serviceCompleted(Long userId, String bookingId, String redirectType) {
        var template = render("SERVICE_COMPLETED_CUSTOMER", NotificationType.SERVICE_COMPLETED,
                "Service Completed", "Your service has been completed successfully.",
                Map.of("orderId", bookingId == null ? "" : bookingId));
        publishBooking(userId, template.notificationType(), template.title(), template.message(), bookingId,
                redirectType, "order-completed:customer:" + bookingId);
    }

    public void partnerServiceCompleted(Long partnerId, String bookingId, BigDecimal earningAmount) {
        String fallbackMessage = earningAmount == null
                ? "Service completed. Your earnings have been updated."
                : "Service completed. Your earnings have been updated: INR " + earningAmount + ".";
        var template = render("SERVICE_COMPLETED_PARTNER", NotificationType.SERVICE_COMPLETED,
                "Service Completed", fallbackMessage,
                Map.of(
                        "orderId", bookingId == null ? "" : bookingId,
                        "earningAmount", earningAmount == null ? "" : earningAmount.toPlainString()
                ));
        publish(partnerId, NotificationUserType.PARTNER, template.notificationType(), NotificationPriority.HIGH,
                template.title(), template.message(), parseReferenceId(bookingId), REFERENCE_TYPE_BOOKING,
                REFERENCE_TYPE_BOOKING, bookingId, "order-completed:partner:" + bookingId);
    }

    public void paymentSuccess(Long userId, String bookingId) {
        publishBooking(userId, NotificationType.PAYMENT_SUCCESSFUL, "Payment Successful",
                "Payment for booking " + bookingId + " completed successfully.", bookingId);
    }

    public void paymentSuccess(Long userId, String bookingId, String redirectType) {
        publishBooking(userId, NotificationType.PAYMENT_SUCCESSFUL, "Payment Successful",
                "Payment for booking " + bookingId + " completed successfully.", bookingId, redirectType, null);
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

    public void accountDeletion(Long userId, String userType, String event, String requestId) {
        NotificationUserType notificationUserType = "PARTNER".equalsIgnoreCase(userType)
                ? NotificationUserType.PARTNER
                : NotificationUserType.CUSTOMER;
        String normalizedEvent = event == null || event.trim().isEmpty() ? "updated" : event.trim().toLowerCase();
        String title = "Account Deletion " + Character.toUpperCase(normalizedEvent.charAt(0)) + normalizedEvent.substring(1);
        String message = "Your account deletion request has been " + normalizedEvent + ".";
        publish(userId, notificationUserType, NotificationType.SYSTEM, NotificationPriority.HIGH,
                title, message, parseReferenceId(requestId), "ACCOUNT_DELETION_REQUEST",
                "ACCOUNT_DELETION_REQUEST", requestId, "account-deletion:" + normalizedEvent + ":" + requestId);
    }

    public void legalPolicyUpdate(Long userId, String userType, String documentType, String version) {
        NotificationUserType notificationUserType = "PARTNER".equalsIgnoreCase(userType)
                ? NotificationUserType.PARTNER
                : NotificationUserType.CUSTOMER;
        publish(userId, notificationUserType, NotificationType.SYSTEM, NotificationPriority.MEDIUM,
                "Legal Policy Updated",
                "A legal policy has been updated: " + documentType + ".",
                null, "LEGAL_DOCUMENT", "LEGAL_DOCUMENT", version,
                "legal-policy:" + documentType + ":" + version);
    }

    public void partnerOnboardingReviewed(Long partnerId,
                                          String applicationStatus,
                                          String documentStatus,
                                          String rejectionReason) {
        OnboardingNotificationCopy copy = onboardingNotificationCopy(applicationStatus, documentStatus, rejectionReason);
        publish(partnerId, NotificationUserType.PARTNER, NotificationType.SYSTEM, NotificationPriority.HIGH,
                copy.title(), copy.message(), partnerId, "ONBOARDING",
                "ONBOARDING", String.valueOf(partnerId),
                "partner-onboarding-review:" + partnerId + ":" + applicationStatus + ":" + documentStatus);
    }

    private void publish(Long userId,
                         NotificationUserType userType,
                         NotificationType type,
                         NotificationPriority priority,
                         String title,
                         String message,
                         Long referenceId,
                         String referenceType) {
        publish(userId, userType, type, priority, title, message, referenceId, referenceType,
                referenceType, referenceId == null ? null : String.valueOf(referenceId), null);
    }

    private void publish(Long userId,
                         NotificationUserType userType,
                         NotificationType type,
                         NotificationPriority priority,
                         String title,
                         String message,
                         Long referenceId,
                         String referenceType,
                         String redirectType,
                         String redirectValue,
                         String idempotencyKeyPrefix) {
        eventPublisher.publishEvent(new SystemNotificationEvent(
                userId,
                userType,
                type,
                priority,
                title,
                message,
                referenceId,
                referenceType,
                redirectType,
                redirectValue,
                idempotencyKeyPrefix
        ));
    }

    private void publishBooking(Long userId, NotificationType type, String title, String message, String bookingId) {
        publishBooking(userId, type, title, message, bookingId, null);
    }

    private void publishBooking(Long userId, NotificationType type, String title, String message, String bookingId,
                                String idempotencyKeyPrefix) {
        publishBooking(userId, type, title, message, bookingId, REFERENCE_TYPE_BOOKING, idempotencyKeyPrefix);
    }

    private void publishBooking(Long userId, NotificationType type, String title, String message, String bookingId,
                                String redirectType, String idempotencyKeyPrefix) {
        eventPublisher.publishEvent(new SystemNotificationEvent(
                userId,
                NotificationUserType.CUSTOMER,
                type,
                NotificationPriority.HIGH,
                title,
                message,
                parseReferenceId(bookingId),
                REFERENCE_TYPE_BOOKING,
                normalizeRedirectType(redirectType),
                bookingId,
                idempotencyKeyPrefix
        ));
    }

    private String normalizeRedirectType(String redirectType) {
        if (redirectType == null || redirectType.isBlank()) {
            return REFERENCE_TYPE_BOOKING;
        }
        return redirectType.trim().toUpperCase();
    }

    private NotificationTemplateService.RenderedTemplate render(String templateName,
                                                               NotificationType fallbackType,
                                                               String fallbackTitle,
                                                               String fallbackMessage,
                                                               Map<String, String> variables) {
        if (notificationTemplateService == null) {
            return new NotificationTemplateService.RenderedTemplate(fallbackType, fallbackTitle, fallbackMessage);
        }
        return notificationTemplateService.render(templateName, fallbackType, fallbackTitle, fallbackMessage, variables);
    }

    private Long parseReferenceId(String value) {
        if (value == null || value.isBlank() || !value.trim().chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private OnboardingNotificationCopy onboardingNotificationCopy(String applicationStatus,
                                                                  String documentStatus,
                                                                  String rejectionReason) {
        String application = applicationStatus == null ? "" : applicationStatus.trim().toUpperCase();
        String document = documentStatus == null ? "" : documentStatus.trim().toUpperCase();
        String reason = rejectionReason == null ? "" : rejectionReason.trim();
        if ("APPROVED".equals(application)) {
            return new OnboardingNotificationCopy("Onboarding approved",
                    "Your onboarding has been approved. You can now start using partner services.");
        }
        if ("REJECTED".equals(application)) {
            return new OnboardingNotificationCopy("Onboarding rejected",
                    reason.isEmpty()
                            ? "Your onboarding was rejected. Please check and resubmit your details."
                            : "Your onboarding was rejected: " + reason);
        }
        if ("BLOCKED".equals(application)) {
            return new OnboardingNotificationCopy("Onboarding blocked",
                    reason.isEmpty()
                            ? "Your onboarding is blocked. Please contact support for next steps."
                            : "Your onboarding is blocked: " + reason);
        }
        if ("REJECTED".equals(document)) {
            return new OnboardingNotificationCopy("Documents need correction",
                    reason.isEmpty()
                            ? "Please update your onboarding documents and submit again."
                            : "Please update your onboarding documents: " + reason);
        }
        if ("UNDER_REVIEW".equals(document)) {
            return new OnboardingNotificationCopy("Documents under review",
                    "Your onboarding documents are under review. We will notify you when the review is complete.");
        }
        if ("VERIFIED".equals(document)) {
            return new OnboardingNotificationCopy("Documents verified",
                    "Your onboarding documents have been verified.");
        }
        return new OnboardingNotificationCopy("Onboarding status updated",
                "Your onboarding status has been updated. Open the app to view details.");
    }

    private record OnboardingNotificationCopy(String title, String message) {
    }
}
