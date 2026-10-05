package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.argThat;

class SystemNotificationEventListenerTest {

    @Test
    void onSystemEventPersistsNotificationsForInAppAndPushAndPublishesDispatchEvents() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SystemNotificationEventListener listener = new SystemNotificationEventListener(notificationRepository, eventPublisher);
        List<Notification> savedNotifications = new ArrayList<>();

        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId((long) (savedNotifications.size() + 1));
            savedNotifications.add(notification);
            return notification;
        }).when(notificationRepository).save(any(Notification.class));

        SystemNotificationEvent event = new SystemNotificationEvent(
                9L,
                NotificationUserType.CUSTOMER,
                NotificationType.BOOKING,
                NotificationPriority.HIGH,
                "Title",
                "Body",
                17L,
                "BOOKING",
                "BOOKING",
                "17"
        );

        listener.onSystemEvent(event);

        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications).extracting(Notification::getChannelType)
                .containsExactly(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(savedNotifications).allSatisfy(notification -> {
            assertThat(notification.getUserId()).isEqualTo(9L);
            assertThat(notification.getUserType()).isEqualTo(NotificationUserType.CUSTOMER);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(notification.getRetryCount()).isZero();
            assertThat(notification.isDeleted()).isFalse();
            assertThat(notification.getRedirectType()).isEqualTo("BOOKING");
            assertThat(notification.getRedirectValue()).isEqualTo("17");
        });

        verify(eventPublisher, times(2)).publishEvent(any(NotificationDispatchEvent.class));
    }

    @Test
    void onSystemEventSkipsNotificationsWhenIdempotencyKeysAlreadyExist() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SystemNotificationEventListener listener = new SystemNotificationEventListener(notificationRepository, eventPublisher);
        org.mockito.Mockito.when(notificationRepository.existsByIdempotencyKey("order-completed:customer:17:IN_APP"))
                .thenReturn(true);
        org.mockito.Mockito.when(notificationRepository.existsByIdempotencyKey("order-completed:customer:17:PUSH"))
                .thenReturn(true);

        SystemNotificationEvent event = new SystemNotificationEvent(
                9L,
                NotificationUserType.CUSTOMER,
                NotificationType.SERVICE_COMPLETED,
                NotificationPriority.HIGH,
                "Service Completed",
                "Your service has been completed successfully.",
                17L,
                "BOOKING",
                "BOOKING",
                "17",
                "order-completed:customer:17"
        );

        listener.onSystemEvent(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(eventPublisher, never()).publishEvent(any(NotificationDispatchEvent.class));
    }

    @Test
    void onSystemEventSetsPerChannelIdempotencyKeys() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SystemNotificationEventListener listener = new SystemNotificationEventListener(notificationRepository, eventPublisher);

        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(notification.getChannelType() == ChannelType.IN_APP ? 1L : 2L);
            return notification;
        }).when(notificationRepository).save(any(Notification.class));

        SystemNotificationEvent event = new SystemNotificationEvent(
                9L,
                NotificationUserType.PARTNER,
                NotificationType.SERVICE_COMPLETED,
                NotificationPriority.HIGH,
                "Service Completed",
                "Service completed. Your earnings have been updated.",
                17L,
                "BOOKING",
                "BOOKING",
                "17",
                "order-completed:partner:17"
        );

        listener.onSystemEvent(event);

        verify(notificationRepository).save(argThat(notification ->
                "order-completed:partner:17:IN_APP".equals(notification.getIdempotencyKey())));
        verify(notificationRepository).save(argThat(notification ->
                "order-completed:partner:17:PUSH".equals(notification.getIdempotencyKey())));
    }
}
