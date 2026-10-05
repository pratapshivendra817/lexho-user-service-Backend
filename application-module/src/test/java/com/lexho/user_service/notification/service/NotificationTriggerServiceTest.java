package com.lexho.user_service.notification.service;

import com.lexho.user_service.notification.event.SystemNotificationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationTriggerServiceTest {

    @Test
    void publishesAllBookingAndNonBookingNotificationShapes() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        NotificationTriggerService service = new NotificationTriggerService(publisher);

        service.bookingCreated(1L, "101");
        service.bookingConfirmed(1L, "bad-id");
        service.providerAssigned(1L, "Asha", "102");
        service.professionalOnTheWay(1L, "Asha", "103");
        service.serviceStarted(1L, "104");
        service.serviceCompleted(1L, "105");
        service.paymentSuccess(1L, "106");
        service.refundInitiated(1L, "107");
        service.subscriptionReminder(1L, " 108 ", "renew");
        service.promotionalOffer(1L, "Offer", "Save");
        service.emergencyAlert(77L, "SOS", "Partner triggered sos", 109L);
        service.partnerServiceCompleted(2L, "110", new java.math.BigDecimal("85.00"));

        ArgumentCaptor<SystemNotificationEvent> captor = ArgumentCaptor.forClass(SystemNotificationEvent.class);
        verify(publisher, times(12)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).hasSize(12);
        assertThat(captor.getAllValues().get(0).getReferenceId()).isEqualTo(101L);
        assertThat(captor.getAllValues().get(1).getReferenceId()).isNull();
        assertThat(captor.getAllValues().get(8).getReferenceId()).isEqualTo(108L);
        assertThat(captor.getAllValues().get(9).getReferenceId()).isNull();
        assertThat(captor.getAllValues().get(10).getUserId()).isEqualTo(77L);
        assertThat(captor.getAllValues().get(10).getRedirectType()).isEqualTo("EMERGENCY_SOS");
        assertThat(captor.getAllValues().get(10).getRedirectValue()).isEqualTo("109");
        assertThat(captor.getAllValues().get(11).getUserType()).isEqualTo(com.lexho.notification.enums.NotificationUserType.PARTNER);
        assertThat(captor.getAllValues().get(11).getIdempotencyKeyPrefix()).isEqualTo("order-completed:partner:110");
    }

    @Test
    void partnerOnboardingReviewPublishesPartnerOnboardingNotificationEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        NotificationTriggerService service = new NotificationTriggerService(publisher);

        service.partnerOnboardingReviewed(41L, "APPROVED", "VERIFIED", null);

        ArgumentCaptor<SystemNotificationEvent> captor = ArgumentCaptor.forClass(SystemNotificationEvent.class);
        verify(publisher).publishEvent(captor.capture());
        SystemNotificationEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(41L);
        assertThat(event.getUserType()).isEqualTo(com.lexho.notification.enums.NotificationUserType.PARTNER);
        assertThat(event.getNotificationType()).isEqualTo(com.lexho.notification.enums.NotificationType.SYSTEM);
        assertThat(event.getTitle()).isEqualTo("Onboarding approved");
        assertThat(event.getMessage()).isEqualTo("Your onboarding has been approved. You can now start using partner services.");
        assertThat(event.getReferenceType()).isEqualTo("ONBOARDING");
        assertThat(event.getRedirectType()).isEqualTo("ONBOARDING");
        assertThat(event.getRedirectValue()).isEqualTo("41");
        assertThat(event.getIdempotencyKeyPrefix()).isEqualTo("partner-onboarding-review:41:APPROVED:VERIFIED");
    }
}
