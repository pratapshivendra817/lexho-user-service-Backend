package com.lexho.user_service.notification.service;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.entity.NotificationLog;
import com.lexho.notification.entity.NotificationPreference;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.DeliveryStatus;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationLogRepository;
import com.lexho.notification.repository.NotificationPreferenceRepository;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.dto.NotificationStatsResponse;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.NotificationSendResponse;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.exception.NotificationNotFoundException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.notification.event.NotificationDispatchEvent;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private UserRepository userRepository;
    @Mock
    private LexhoUserProfileRepository lexhoUserProfileRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private PartnerLocalizationService localizationService;

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

    @Test
    void markFailedPermanentlySetsMaxRetryCount() {
        Notification notification = new Notification();
        notification.setId(88L);
        notification.setUserId(100L);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationManagementService.markFailedPermanently(notification, "No active device token found for push notification");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals(NotificationStatus.FAILED, notificationCaptor.getValue().getStatus());
        assertEquals(3, notificationCaptor.getValue().getRetryCount());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertEquals(DeliveryStatus.FAILED, logCaptor.getValue().getDeliveryStatus());
        assertEquals(3, logCaptor.getValue().getRetryCount());
        assertEquals("No active device token found for push notification", logCaptor.getValue().getErrorMessage());
    }

    @Test
    void customerNotificationFeedUsesOnlyInAppNotifications() {
        Notification notification = new Notification();
        notification.setId(90L);
        notification.setUserId(100L);
        notification.setChannelType(ChannelType.IN_APP);
        notification.setStatus(NotificationStatus.SENT);

        NotificationResponse mapped = new NotificationResponse();
        mapped.setId(90L);
        mapped.setChannelType(ChannelType.IN_APP);

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(notificationRepository.findByUserIdAndChannelTypeAndDeletedFalseOrderByCreatedAtDesc(100L, ChannelType.IN_APP))
                .thenReturn(List.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(mapped);
        when(notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(90L)).thenReturn(List.of());

        NotificationPageResponse<NotificationResponse> response =
                notificationManagementService.getCustomerNotifications(0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals(ChannelType.IN_APP, response.getContent().get(0).getChannelType());
        verify(notificationRepository).findByUserIdAndChannelTypeAndDeletedFalseOrderByCreatedAtDesc(100L, ChannelType.IN_APP);
    }

    @Test
    void currentUserOperationsRejectMissingAuthentication() {
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(NotificationAccessDeniedException.class,
                () -> notificationManagementService.getProviderNotifications(0, 10));
        assertThrows(NotificationAccessDeniedException.class,
                () -> notificationManagementService.unreadCountForCurrentUser());
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(notificationRepository.countByUserIdAndChannelTypeAndStatusNotAndDeletedFalse(
                12L, ChannelType.IN_APP, NotificationStatus.READ)).thenReturn(4L);

        assertEquals(4L, notificationManagementService.unreadCountByUserId(12L));
    }

    @Test
    void softDeleteOwnNotificationPersistsDeletedFlag() {
        Notification notification = notification(31L, 7L, NotificationStatus.SENT);
        when(notificationRepository.findByIdAndDeletedFalse(31L)).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(7L));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Notification result = notificationManagementService.softDeleteForCurrentUser(31L);

        assertTrue(result.isDeleted());
        verify(notificationRepository).save(notification);
    }

    @Test
    void findActiveThrowsForMissingNotification() {
        when(notificationRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationManagementService.findActive(404L));
    }

    @Test
    void retryFailedNotificationIncrementsCountAndPublishesEvent() {
        Notification notification = notification(40L, 9L, NotificationStatus.FAILED);
        notification.setRetryCount(null);
        notification.setChannelType(ChannelType.EMAIL);
        NotificationResponse mapped = new NotificationResponse();
        mapped.setId(40L);
        when(notificationRepository.findByIdAndDeletedFalse(40L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(mapped);
        when(notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(40L)).thenReturn(List.of());

        NotificationResponse result = notificationManagementService.retryNotification(40L);

        assertEquals(40L, result.getId());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        verify(eventPublisher).publishEvent(any(NotificationDispatchEvent.class));
    }

    @Test
    void retryRejectsDeliveredAndExhaustedNotifications() {
        Notification delivered = notification(41L, 9L, NotificationStatus.DELIVERED);
        delivered.setRetryCount(0);
        Notification exhausted = notification(42L, 9L, NotificationStatus.FAILED);
        exhausted.setRetryCount(3);
        when(notificationRepository.findByIdAndDeletedFalse(41L)).thenReturn(Optional.of(delivered));
        when(notificationRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(exhausted));

        assertThrows(NotificationValidationException.class,
                () -> notificationManagementService.retryNotification(41L));
        assertThrows(NotificationValidationException.class,
                () -> notificationManagementService.retryNotification(42L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void adminSearchNormalizesPagingSortingAndBlankSearch() {
        Notification notification = notification(50L, 10L, NotificationStatus.SENT);
        NotificationResponse mapped = new NotificationResponse();
        mapped.setId(50L);
        when(notificationRepository.searchAdmin(eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(mapped);
        when(notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());

        NotificationPageResponse<NotificationResponse> result = notificationManagementService.getAdminNotifications(
                -3, 500, "unsupported", "asc", null, null, null, null, "   ");

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).searchAdmin(eq(null), eq(null), eq(null), eq(null), eq(null), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        assertNotNull(pageable.getValue().getSort().getOrderFor("createdAt"));
        assertTrue(pageable.getValue().getSort().getOrderFor("createdAt").isAscending());
    }

    @Test
    void userFeedFiltersExpiredItemsAndClampsPaging() {
        Notification active = notification(60L, 20L, NotificationStatus.SENT);
        active.setExpiryTime(LocalDateTime.now().plusHours(1));
        Notification expired = notification(61L, 20L, NotificationStatus.SENT);
        expired.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        NotificationResponse mapped = new NotificationResponse();
        mapped.setId(60L);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(notificationRepository.findByUserIdAndChannelTypeAndDeletedFalseOrderByCreatedAtDesc(20L, ChannelType.IN_APP))
                .thenReturn(List.of(active, expired));
        when(notificationMapper.toResponse(active)).thenReturn(mapped);
        when(notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(60L)).thenReturn(List.of());

        NotificationPageResponse<NotificationResponse> result =
                notificationManagementService.getProviderNotifications(-1, 0);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void scheduledRetryAndCleanupJobsProcessRepositoryResults() {
        Notification queued = notification(70L, 30L, NotificationStatus.QUEUED);
        queued.setChannelType(ChannelType.PUSH);
        queued.setRetryCount(0);
        Notification failed = notification(71L, 30L, NotificationStatus.FAILED);
        failed.setChannelType(ChannelType.SMS);
        failed.setRetryCount(1);
        Notification expired = notification(72L, 30L, NotificationStatus.SENT);
        when(notificationRepository.findByStatusAndScheduledTimeLessThanEqualAndDeletedFalse(
                eq(NotificationStatus.QUEUED), any(LocalDateTime.class))).thenReturn(List.of(queued));
        when(notificationRepository.findByStatusAndDeletedFalseAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .thenReturn(List.of(failed));
        when(notificationRepository.save(failed)).thenReturn(failed);
        when(notificationRepository.findByExpiryTimeBeforeAndDeletedFalse(any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        notificationManagementService.processScheduledNotifications();
        notificationManagementService.retryFailedNotifications();
        notificationManagementService.cleanupExpiredNotifications();

        assertEquals(NotificationStatus.QUEUED, failed.getStatus());
        assertEquals(2, failed.getRetryCount());
        assertTrue(expired.isDeleted());
        verify(eventPublisher, times(2)).publishEvent(any(NotificationDispatchEvent.class));
        verify(notificationRepository).saveAll(List.of(expired));
    }

    @Test
    void dispatchAndFailureUpdatesPersistLogsWithFallbackMessages() {
        Notification delivered = notification(80L, 40L, NotificationStatus.PENDING);
        delivered.setRetryCount(null);
        Notification failed = notification(81L, 40L, NotificationStatus.PENDING);
        failed.setRetryCount(2);

        notificationManagementService.markDispatched(delivered);
        notificationManagementService.markFailed(failed, "  ");

        assertEquals(NotificationStatus.DELIVERED, delivered.getStatus());
        assertNotNull(delivered.getSentAt());
        assertNotNull(delivered.getDeliveredAt());
        assertEquals(NotificationStatus.FAILED, failed.getStatus());
        ArgumentCaptor<NotificationLog> logs = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(3)).save(logs.capture());
        assertEquals("Dispatch failed due to unknown error", logs.getAllValues().get(2).getErrorMessage());
    }

    @Test
    void bulkRequestRejectsInvalidExpiryAndReturnsNoTargets() {
        AdminBulkNotificationRequest invalid = baseRequest();
        invalid.setScheduledTime(LocalDateTime.now().plusHours(2));
        invalid.setExpiryTime(LocalDateTime.now().plusHours(1));
        assertThrows(NotificationValidationException.class,
                () -> notificationManagementService.sendBulkNotification(invalid));

        AdminBulkNotificationRequest empty = baseRequest();
        when(lexhoUserProfileRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        NotificationSendResponse result = notificationManagementService.sendBulkNotification(empty);

        assertEquals(0, result.getTotalTargets());
        assertEquals(0, result.getTotalCreated());
    }

    @Test
    void statsAndAdminNotificationShouldAggregateAndExposeFailureReason() {
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.SENT)).thenReturn(2L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.DELIVERED)).thenReturn(3L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.FAILED)).thenReturn(1L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.READ)).thenReturn(4L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.PENDING)).thenReturn(5L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.QUEUED)).thenReturn(6L);
        when(notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.CLICKED)).thenReturn(7L);
        for (ChannelType type : ChannelType.values()) {
            when(notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(type, NotificationStatus.SENT)).thenReturn(1L);
            when(notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(type, NotificationStatus.DELIVERED)).thenReturn(1L);
            when(notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(type, NotificationStatus.READ)).thenReturn(1L);
            when(notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(type, NotificationStatus.CLICKED)).thenReturn(1L);
            when(notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(type, NotificationStatus.FAILED)).thenReturn(2L);
        }

        Notification notification = notification(99L, 7L, NotificationStatus.FAILED);
        NotificationResponse mapped = new NotificationResponse();
        mapped.setId(99L);
        when(notificationRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(mapped);
        NotificationLog log = new NotificationLog();
        log.setErrorMessage(" failed ");
        when(notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(99L)).thenReturn(List.of(log));

        NotificationStatsResponse stats = notificationManagementService.getStats();
        NotificationResponse response = notificationManagementService.getAdminNotificationById(99L);

        assertEquals(5L, stats.getTotalSent());
        assertEquals(11L, stats.getTotalPending());
        assertEquals(ChannelType.values().length, stats.getChannelWiseStats().size());
        assertEquals("failed", response.getFailureReason());
    }

    @Test
    void sendBulkShouldHonorPreferencesRateLimitDuplicateGuardAndVariables() {
        AdminBulkNotificationRequest request = baseRequest();
        request.setMessage("Hi {name}");
        request.setChannels(List.of(ChannelType.IN_APP, ChannelType.PUSH, ChannelType.EMAIL));
        request.setTemplateVariables(new HashMap<>(Collections.singletonMap("name", "Lexho")));
        request.setUserIds(List.of(10L));

        NotificationPreference preference = new NotificationPreference();
        preference.setPromotionalEnabled(true);
        preference.setBookingEnabled(true);
        preference.setPushEnabled(false);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(true);
        preference.setWhatsappEnabled(true);
        when(notificationPreferenceRepository.findByUserId(10L)).thenReturn(Optional.of(preference));

        Notification recent = notification(1L, 10L, NotificationStatus.SENT);
        recent.setNotificationType(NotificationType.REMINDER);
        recent.setCreatedAt(LocalDateTime.now());
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(recent, recent, recent, recent, recent));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(111L);
            return saved;
        });

        NotificationSendResponse rateLimited = notificationManagementService.sendBulkNotification(request);
        assertEquals(0, rateLimited.getTotalCreated());

        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(notificationRepository.existsRecentDuplicate(eq(10L), eq(NotificationUserType.CUSTOMER), eq(NotificationType.REMINDER),
                any(), eq("Notice"), eq("Hi Lexho"), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(true);
        NotificationSendResponse duplicateSkipped = notificationManagementService.sendBulkNotification(request);
        assertEquals(0, duplicateSkipped.getTotalCreated());

        when(notificationRepository.existsRecentDuplicate(eq(10L), eq(NotificationUserType.CUSTOMER), eq(NotificationType.REMINDER),
                any(), eq("Notice"), eq("Hi Lexho"), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(false);
        NotificationSendResponse created = notificationManagementService.sendBulkNotification(request);
        assertEquals(2, created.getTotalCreated());
        verify(notificationRepository, atLeastOnce()).save(argThat(item ->
                item.getChannelType() == ChannelType.IN_APP || "Hi Lexho".equals(item.getMessage())));
    }

    @Test
    void sendBulkShouldResolveAdminAndFilteredCustomerTargets() {
        AdminBulkNotificationRequest adminRequest = baseRequest();
        adminRequest.setTargetUserType(NotificationUserType.ADMIN);
        com.lexho.admin.entity.Admin admin = new com.lexho.admin.entity.Admin();
        admin.setId(9L);
        when(adminRepository.findAll()).thenReturn(List.of(admin));
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(9L)).thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(1, notificationManagementService.sendBulkNotification(adminRequest).getTotalTargets());

        AdminBulkNotificationRequest filteredRequest = baseRequest();
        filteredRequest.setTargetUserType(NotificationUserType.CUSTOMER);
        filteredRequest.setCity("gurugram");
        filteredRequest.setInactiveUsers(true);
        filteredRequest.setPremiumUsers(true);
        User inactivePremium = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(inactivePremium, "id", 17L);
        inactivePremium.setDeleted(false);
        inactivePremium.setRole(Role.CUSTOMER);
        inactivePremium.setCity("Gurugram");
        inactivePremium.setStatus(UserStatus.INACTIVE);
        inactivePremium.setTotalBookings(60L);
        when(userRepository.findAll()).thenReturn(List.of(inactivePremium));
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(17L)).thenReturn(List.of());
        when(lexhoUserProfileRepository.findAll()).thenReturn(List.of());
        assertEquals(1, notificationManagementService.sendBulkNotification(filteredRequest).getTotalTargets());

        AdminBulkNotificationRequest customerRequest = baseRequest();
        customerRequest.setTargetUserType(NotificationUserType.CUSTOMER);
        LexhoUserProfile profile = new LexhoUserProfile();
        org.springframework.test.util.ReflectionTestUtils.setField(profile, "id", 5L);
        profile.setRole(Role.CUSTOMER);
        when(lexhoUserProfileRepository.findAll()).thenReturn(List.of(profile));
        when(userRepository.findAll()).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        assertEquals(1, notificationManagementService.sendBulkNotification(customerRequest).getTotalTargets());
    }

    @Test
    void retryShouldLogUnexpectedPublishFailureAndPastScheduleShouldNormalize() {
        Notification notification = notification(55L, 7L, NotificationStatus.PENDING);
        notification.setRetryCount(0);
        notification.setChannelType(ChannelType.SMS);
        when(notificationRepository.findByIdAndDeletedFalse(55L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(new NotificationResponse());
        doThrow(new IllegalStateException("queue down")).when(eventPublisher).publishEvent(any(NotificationDispatchEvent.class));

        NotificationResponse response = notificationManagementService.retryNotification(55L);
        assertNotNull(response);
        verify(notificationLogRepository).save(argThat(log -> "FAILED".equals(log.getDeliveryStatus().name())
                && log.getErrorMessage() != null
                && log.getErrorMessage().contains("queue down")));

        AdminBulkNotificationRequest request = baseRequest();
        request.setScheduledTime(LocalDateTime.now().minusHours(1));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(notificationManagementService, "validateBulkRequest", request);
        assertEquals(null, request.getScheduledTime());
    }

    @Test
    void warningDisputeStatusNotificationShouldCreateInAppAndPushNotifications() {
        when(notificationRepository.existsRecentDuplicate(eq(7L), eq(NotificationUserType.PARTNER), eq(NotificationType.SYSTEM),
                any(), any(), any(), eq("WARNING"), eq(55L), eq("WARNING"), eq("55"), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(saved.getChannelType() == ChannelType.IN_APP ? 201L : 202L);
            return saved;
        });

        notificationManagementService.createPartnerWarningDisputeStatusNotification(7L, 55L, "APPROVED");

        verify(notificationRepository, times(2)).save(argThat(notification ->
                notification.getUserId().equals(7L)
                        && "WARNING".equals(notification.getRedirectType())
                        && "55".equals(notification.getRedirectValue())
                        && NotificationType.SYSTEM == notification.getNotificationType()));
        verify(eventPublisher, times(2)).publishEvent(any(NotificationDispatchEvent.class));
    }

    private AdminBulkNotificationRequest baseRequest() {
        AdminBulkNotificationRequest request = new AdminBulkNotificationRequest();
        request.setTitle("Notice");
        request.setMessage("Message");
        request.setNotificationType(NotificationType.REMINDER);
        request.setChannels(List.of(ChannelType.IN_APP));
        request.setTargetUserType(NotificationUserType.CUSTOMER);
        return request;
    }

    private Notification notification(Long id, Long userId, NotificationStatus status) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setStatus(status);
        notification.setDeleted(false);
        return notification;
    }
}
