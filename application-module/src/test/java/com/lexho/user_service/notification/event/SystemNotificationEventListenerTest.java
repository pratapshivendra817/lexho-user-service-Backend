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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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
}
