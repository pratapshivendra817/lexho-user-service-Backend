package com.lexho.user_service.service;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.notification.service.PushTokenService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void sendPersistsNotificationAndIteratesPushTokens() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);

        when(pushTokenService.activeTokens(7L, NotificationUserType.CUSTOMER.name()))
                .thenReturn(List.of("abcdefgh123", "z"));

        service.send(7L, "Title", "Message", "order", 99L);

        verify(notificationRepository).save(any(Notification.class));
        verify(pushTokenService).activeTokens(7L, NotificationUserType.CUSTOMER.name());
    }

    @Test
    void sendDefaultsToSystemTypeWhenTypeIsNull() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);

        service.send(9L, "Title", "Message", null, 5L);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getAllUnreadAndCountDelegateToRepository() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);
        List<Notification> notifications = List.of(new Notification());

        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(1L)).thenReturn(notifications);
        when(notificationRepository.findByUserIdAndStatusNotAndDeletedFalseOrderByCreatedAtDesc(1L, NotificationStatus.READ))
                .thenReturn(notifications);
        when(notificationRepository.countByUserIdAndStatusNotAndDeletedFalse(1L, NotificationStatus.READ)).thenReturn(3L);

        assertThat(service.getAll(1L)).isSameAs(notifications);
        assertThat(service.getUnread(1L)).isSameAs(notifications);
        assertThat(service.unreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void markReadUpdatesOwnedNotification() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);
        Notification notification = new Notification();
        notification.setUserId(5L);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(notification));

        service.markRead(5L, 11L);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markReadRejectsMissingOrForeignNotification() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);
        Notification notification = new Notification();
        notification.setUserId(9L);

        when(notificationRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(5L, 12L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        when(notificationRepository.findByIdAndDeletedFalse(13L)).thenReturn(Optional.of(notification));
        assertThatThrownBy(() -> service.markRead(5L, 13L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void deleteNotificationMarksOwnedNotificationDeleted() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);
        Notification notification = new Notification();
        notification.setUserId(5L);

        when(notificationRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(notification));

        service.deleteNotification(5L, 11L);

        assertThat(notification.isDeleted()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void deleteNotificationRejectsMissingOrForeignNotification() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PushTokenService pushTokenService = mock(PushTokenService.class);
        NotificationService service = new NotificationService(notificationRepository, pushTokenService);
        Notification notification = new Notification();
        notification.setUserId(9L);

        when(notificationRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteNotification(5L, 12L))
                .isInstanceOf(ResourceNotFoundException.class);

        when(notificationRepository.findByIdAndDeletedFalse(13L)).thenReturn(Optional.of(notification));
        assertThatThrownBy(() -> service.deleteNotification(5L, 13L))
                .isInstanceOf(UnauthorizedException.class);
    }
}
