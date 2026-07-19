package com.lexho.user_service.notification.controller;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.user_service.lexhopartner.controller.LexhoPartnerNotificationController;
import com.lexho.user_service.notification.dto.*;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.PushTokenService;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllersTest {

    @Mock private NotificationManagementService managementService;
    @Mock private SecurityUtils securityUtils;
    @Mock private PushTokenService pushTokenService;
    @Mock private NotificationMapper notificationMapper;

    private AdminNotificationController adminController;
    private NotificationSendController sendController;
    private PushTokenController pushTokenController;
    private LexhoPartnerNotificationController partnerController;

    @BeforeEach
    void setUp() {
        adminController = new AdminNotificationController(managementService);
        sendController = new NotificationSendController(managementService, securityUtils);
        pushTokenController = new PushTokenController(pushTokenService);
        partnerController = new LexhoPartnerNotificationController(managementService, notificationMapper);
    }

    @Test
    void adminSendReadRetryAndStatsEndpointsDelegate() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();
        NotificationSendResponse sent = mock(NotificationSendResponse.class);
        when(managementService.sendBulkNotification(request)).thenReturn(sent);
        NotificationResponse notification = new NotificationResponse();
        notification.setId(5L);
        when(managementService.getAdminNotificationById(5L)).thenReturn(notification);
        when(managementService.retryNotification(5L)).thenReturn(notification);
        NotificationStatsResponse stats = mock(NotificationStatsResponse.class);
        when(managementService.getStats()).thenReturn(stats);

        assertThat(adminController.send(request).getBody()).isSameAs(sent);
        assertThat(adminController.sendLegacy(request).getBody()).isSameAs(sent);
        assertThat(adminController.byId(5L).getBody()).isSameAs(notification);
        assertThat(adminController.retry(5L).getBody()).isSameAs(notification);
        assertThat(adminController.stats().getBody()).isSameAs(stats);
    }

    @Test
    void adminFiltersNormalizeAliasesAndRejectInvalidValues() {
        NotificationPageResponse<NotificationResponse> page =
                new NotificationPageResponse<>(List.of(), 0, 0, 0);
        when(managementService.getAdminNotifications(
                1, 5, "createdAt", "asc", NotificationUserType.CUSTOMER,
                NotificationType.PROMOTIONAL_OFFER, null, null, "sale")).thenReturn(page);

        assertThat(adminController.all(1, 5, "createdAt", "asc",
                "customers", "promotional-offers", null, null, "sale").getBody()).isSameAs(page);

        adminController.all(0, 10, "createdAt", "desc", "partner", "payment-success", null, null, null);
        adminController.all(0, 10, "createdAt", "desc", "admins", null, null, null, null);
        adminController.all(0, 10, "createdAt", "desc", " ", " ", null, null, null);

        assertThatThrownBy(() -> adminController.all(0, 10, "createdAt", "desc",
                "unknown", null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adminController.all(0, 10, "createdAt", "desc",
                null, "unknown-type", null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retryPreservesValidationExceptionAndWrapsUnexpectedFailure() {
        when(managementService.retryNotification(1L))
                .thenThrow(new NotificationValidationException("not failed"));
        when(managementService.retryNotification(2L))
                .thenThrow(new IllegalStateException("provider down"));

        assertThatThrownBy(() -> adminController.retry(1L))
                .isInstanceOf(NotificationValidationException.class).hasMessage("not failed");
        assertThatThrownBy(() -> adminController.retry(2L))
                .isInstanceOf(NotificationValidationException.class)
                .hasMessage("Retry failed: provider down");
    }

    @Test
    void sendBuildsBulkRequestWithNumericBookingReference() {
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(7L));
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        NotificationSendResponse response = mock(NotificationSendResponse.class);
        when(managementService.sendBulkNotification(any())).thenReturn(response);
        PushSendRequest request = pushRequest(7L, " tracking ", " 123 ");

        assertThat(sendController.send(request).getData()).isSameAs(response);

        ArgumentCaptor<AdminBulkNotificationRequest> captor =
                ArgumentCaptor.forClass(AdminBulkNotificationRequest.class);
        verify(managementService).sendBulkNotification(captor.capture());
        assertThat(captor.getValue().getTargetUserType()).isEqualTo(NotificationUserType.CUSTOMER);
        assertThat(captor.getValue().getReferenceType()).isEqualTo("BOOKING");
        assertThat(captor.getValue().getReferenceId()).isEqualTo(123L);
        assertThat(captor.getValue().getRedirectValue()).isEqualTo("123");
    }

    @Test
    void sendAllowsAdminAndNonNumericBookingButRejectsOwnershipAndRedirectFailures() {
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("ADMIN"));
        when(managementService.sendBulkNotification(any())).thenReturn(mock(NotificationSendResponse.class));

        sendController.send(pushRequest(9L, "PAYMENT", "booking-A"));

        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("PARTNER"));
        assertThatThrownBy(() -> sendController.send(pushRequest(9L, "PAYMENT", null)))
                .isInstanceOf(NotificationAccessDeniedException.class);

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sendController.send(pushRequest(9L, "unsupported", null)))
                .isInstanceOf(NotificationValidationException.class).hasMessage("Unsupported redirectType");
        PushSendRequest missingData = new PushSendRequest();
        missingData.setUserId(9L);
        assertThatThrownBy(() -> sendController.send(missingData))
                .isInstanceOf(NotificationValidationException.class)
                .hasMessage("data.redirectType is required");
    }

    @Test
    void pushTokenAndPartnerNotificationEndpointsDelegate() {
        PushTokenUpsertRequest token = new PushTokenUpsertRequest();
        token.deviceId = "phone-1";
        token.token = "token";
        assertThat(pushTokenController.register(7L, "CUSTOMER", token).getData())
                .containsEntry("deviceId", "phone-1");
        assertThat(pushTokenController.unregister(7L, "CUSTOMER", "phone-1").isSuccess()).isTrue();
        verify(pushTokenService).upsert(7L, "CUSTOMER", token);
        verify(pushTokenService).deactivate(7L, "CUSTOMER", "phone-1");

        NotificationPageResponse<NotificationResponse> page =
                new NotificationPageResponse<>(List.of(), 0, 0, 0);
        when(managementService.getProviderNotifications(1, 5)).thenReturn(page);
        Notification entity = mock(Notification.class);
        NotificationResponse mapped = new NotificationResponse();
        when(managementService.markAsReadForCurrentUser(8L)).thenReturn(entity);
        when(notificationMapper.toResponse(entity)).thenReturn(mapped);
        when(managementService.unreadCountForCurrentUser()).thenReturn(4L);

        assertThat(partnerController.notifications(1, 5).getData()).isSameAs(page);
        assertThat(partnerController.markRead(8L).getData()).isSameAs(mapped);
        assertThat(partnerController.unreadCount().getData().getUnreadCount()).isEqualTo(4L);
    }

    private PushSendRequest pushRequest(Long userId, String redirectType, String bookingId) {
        PushSendRequest request = new PushSendRequest();
        request.setUserId(userId);
        request.setTitle("Title");
        request.setMessage("Message");
        PushSendRequest.PushSendData data = new PushSendRequest.PushSendData();
        data.setRedirectType(redirectType);
        data.setBookingId(bookingId);
        request.setData(data);
        return request;
    }
}
