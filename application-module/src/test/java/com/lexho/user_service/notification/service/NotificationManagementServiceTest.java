package com.lexho.user_service.notification.service;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationLogRepository;
import com.lexho.notification.repository.NotificationPreferenceRepository;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.notification.repository.UserDeviceTokenRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.dto.NotificationSendResponse;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationManagementServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private NotificationManagementService notificationManagementService;

    @Test
    void sendBulkCreatesNotificationsForProvidedUsers() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();
        request.setTitle("Service Reminder");
        request.setMessage("Your booking is tomorrow");
        request.setNotificationType(NotificationType.REMINDER);
        request.setPriority(NotificationPriority.MEDIUM);
        request.setChannels(List.of(ChannelType.IN_APP));
        request.setTargetUserType(NotificationUserType.CUSTOMER);
        request.setUserIds(List.of(10L, 20L));

        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(20L)).thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        NotificationSendResponse response = notificationManagementService.sendBulkNotification(request);

        assertEquals(2, response.getTotalTargets());
        assertEquals(2, response.getTotalCreated());
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    void markReadRejectsWhenNotificationBelongsToAnotherUser() {
        Notification notification = new Notification();
        notification.setId(77L);
        notification.setUserId(100L);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByIdAndDeletedFalse(77L)).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(101L));

        assertThrows(NotificationAccessDeniedException.class,
                () -> notificationManagementService.markAsReadForCurrentUser(77L));
    }

    @Test
    void markReadUpdatesStatusAndReadAt() {
        Notification notification = new Notification();
        notification.setId(77L);
        notification.setUserId(100L);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByIdAndDeletedFalse(77L)).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationManagementService.markAsReadForCurrentUser(77L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationStatus.READ, captor.getValue().getStatus());
    }
}
