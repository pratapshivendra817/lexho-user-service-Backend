package com.lexho.user_service.notification.event;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.notification.service.EmailNotificationService;
import com.lexho.user_service.notification.service.FcmPushService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.PushTokenService;
import com.lexho.user_service.notification.service.SmsNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchListenerTest {

    @Mock
    private NotificationManagementService managementService;
    @Mock
    private PushTokenService pushTokenService;
    @Mock
    private FcmPushService fcmPushService;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private SmsNotificationService smsService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationDispatchListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationDispatchListener(managementService, pushTokenService, fcmPushService,
                emailService, smsService, messagingTemplate);
    }

    @Test
    void dispatchesInAppToCustomerAndAdminTopics() {
        Notification notification = notification(NotificationUserType.CUSTOMER);
        when(managementService.findActive(1L)).thenReturn(notification);

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.IN_APP));

        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/7"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/notifications"), any(Object.class));
        verify(managementService).markDispatched(notification);
    }

    @Test
    void dispatchesPushWithRoleAndReferenceData() {
        Notification notification = notification(NotificationUserType.PARTNER);
        notification.setReferenceType("BOOKING");
        notification.setReferenceId(91L);
        notification.setRedirectType("DETAIL");
        notification.setRedirectValue("/bookings/91");
        when(managementService.findActive(1L)).thenReturn(notification);
        when(pushTokenService.activeTokens(7L, "PARTNER")).thenReturn(List.of("a", "b"));
        when(fcmPushService.sendToTokens(eq(List.of("a", "b")), eq("Title"), eq("Message"), any()))
                .thenReturn(2);

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.PUSH));

        verify(fcmPushService).sendToTokens(eq(List.of("a", "b")), eq("Title"), eq("Message"), any());
        verify(managementService).markDispatched(notification);
    }

    @Test
    void dispatchesWarningPushWithWarningRoutingData() {
        Notification notification = notification(NotificationUserType.PARTNER);
        notification.setRedirectType("WARNING");
        notification.setRedirectValue("456");
        when(managementService.findActive(1L)).thenReturn(notification);
        when(pushTokenService.activeTokens(7L, "PARTNER")).thenReturn(List.of("token"));
        when(fcmPushService.sendToTokens(eq(List.of("token")), eq("Title"), eq("Message"), any()))
                .thenReturn(1);

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.PUSH));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fcmPushService).sendToTokens(eq(List.of("token")), eq("Title"), eq("Message"), captor.capture());
        assertThat(captor.getValue()).containsEntry("redirectType", "WARNING")
                .containsEntry("redirectValue", "456")
                .containsEntry("warningId", "456")
                .containsEntry("title", "Title")
                .containsEntry("message", "Message");
    }

    @Test
    void dispatchesEmailSmsAndWhatsappChannels() {
        Notification notification = notification(null);
        when(managementService.findActive(1L)).thenReturn(notification);

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.EMAIL));
        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.SMS));
        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.WHATSAPP));

        verify(emailService).send(notification);
        verify(smsService).send(notification);
        verify(managementService, org.mockito.Mockito.times(3)).markDispatched(notification);
    }

    @Test
    void emptyPushTokensCausePermanentFailure() {
        Notification notification = notification(null);
        when(managementService.findActive(1L)).thenReturn(notification);
        when(pushTokenService.activeTokens(7L, "CUSTOMER")).thenReturn(List.of());

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.PUSH));

        verify(managementService).markFailedPermanently(eq(notification), contains("No active device token"));
        verify(managementService, never()).markDispatched(notification);
    }

    @Test
    void zeroSuccessfulPushesCauseRetryableFailure() {
        Notification notification = notification(NotificationUserType.ADMIN);
        when(managementService.findActive(1L)).thenReturn(notification);
        when(pushTokenService.activeTokens(7L, "ADMIN")).thenReturn(List.of("token"));
        when(fcmPushService.sendToTokens(eq(List.of("token")), any(), any(), any())).thenReturn(0);

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.PUSH));

        verify(managementService).markFailed(eq(notification), contains("failed for all tokens"));
        verify(managementService, never()).markDispatched(notification);
    }

    @Test
    void repeatedLookupFailureDoesNotAttemptStatusUpdate() {
        when(managementService.findActive(1L)).thenThrow(new IllegalStateException("database unavailable"));

        listener.onDispatch(new NotificationDispatchEvent(1L, ChannelType.EMAIL));

        verify(managementService, never()).markFailed(any(), any());
        verify(managementService, never()).markFailedPermanently(any(), any());
    }

    private Notification notification(NotificationUserType userType) {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(7L);
        notification.setUserType(userType);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setNotificationType(NotificationType.BOOKING);
        return notification;
    }
}
