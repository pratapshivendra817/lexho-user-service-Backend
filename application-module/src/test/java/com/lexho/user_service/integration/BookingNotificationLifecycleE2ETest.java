package com.lexho.user_service.integration;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.entity.NotificationLog;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.DeliveryStatus;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationLogRepository;
import com.lexho.notification.repository.NotificationPreferenceRepository;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.notification.dto.PushTokenUpsertRequest;
import com.lexho.user_service.notification.entity.PushDeviceTokenEntity;
import com.lexho.user_service.notification.event.NotificationDispatchEvent;
import com.lexho.user_service.notification.event.NotificationDispatchListener;
import com.lexho.user_service.notification.event.SystemNotificationEvent;
import com.lexho.user_service.notification.event.SystemNotificationEventListener;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.repository.PushDeviceTokenRepository;
import com.lexho.user_service.notification.service.EmailNotificationService;
import com.lexho.user_service.notification.service.FcmPushService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import com.lexho.user_service.notification.service.PushTokenService;
import com.lexho.user_service.notification.service.SmsNotificationService;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = BookingNotificationLifecycleE2ETest.JpaConfig.class)
class BookingNotificationLifecycleE2ETest {

    private static final Long CUSTOMER_ID = 1001L;
    private static final Long PARTNER_ID = 2002L;
    private static final String BOOKING_ID = "7001";
    private static final String SCHEDULED_BOOKING_ID = "7002";
    private static final String SUBSCRIPTION_ID = "8001";

    @jakarta.annotation.Resource
    private NotificationRepository notificationRepository;

    @jakarta.annotation.Resource
    private NotificationLogRepository notificationLogRepository;

    @jakarta.annotation.Resource
    private PushDeviceTokenRepository pushDeviceTokenRepository;

    private FcmPushService fcmPushService;
    private NotificationTriggerService triggerService;

    @BeforeEach
    void setUp() {
        fcmPushService = mock(FcmPushService.class);
        when(fcmPushService.sendToTokens(any(), any(), any(), any()))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());

        PushTokenService pushTokenService = new PushTokenService(pushDeviceTokenRepository);
        upsertToken(pushTokenService, CUSTOMER_ID, "CUSTOMER", "customer-device", "customer-fcm-token");
        upsertToken(pushTokenService, PARTNER_ID, "PARTNER", "partner-device", "partner-fcm-token");

        EventRouter eventRouter = new EventRouter();
        NotificationManagementService managementService = notificationManagementService(eventRouter);
        NotificationDispatchListener dispatchListener = new NotificationDispatchListener(
                managementService,
                pushTokenService,
                fcmPushService,
                mock(EmailNotificationService.class),
                mock(SmsNotificationService.class),
                mock(NotificationRealtimeService.class),
                mock(SimpMessagingTemplate.class)
        );
        SystemNotificationEventListener systemListener =
                new SystemNotificationEventListener(notificationRepository, eventRouter);
        eventRouter.systemListener = systemListener;
        eventRouter.dispatchListener = dispatchListener;
        triggerService = new NotificationTriggerService(eventRouter);
    }

    @Test
    void bookingLifecyclePersistsInAppAndPushRowsDispatchesFcmAndDeduplicatesCompletion() {
        triggerService.bookingCreated(CUSTOMER_ID, BOOKING_ID, "INSTANT_BOOKING");
        triggerService.paymentSuccess(CUSTOMER_ID, BOOKING_ID, "INSTANT_BOOKING");
        triggerService.serviceStarted(CUSTOMER_ID, BOOKING_ID, "INSTANT_BOOKING");
        triggerService.serviceCompleted(CUSTOMER_ID, BOOKING_ID, "INSTANT_BOOKING");
        triggerService.serviceCompleted(CUSTOMER_ID, BOOKING_ID, "INSTANT_BOOKING");
        triggerService.partnerServiceCompleted(PARTNER_ID, BOOKING_ID, new BigDecimal("185.50"));
        triggerService.partnerServiceCompleted(PARTNER_ID, BOOKING_ID, new BigDecimal("185.50"));

        List<Notification> customerRows =
                notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(CUSTOMER_ID);
        List<Notification> partnerRows =
                notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(PARTNER_ID);

        assertThat(customerRows).hasSize(8);
        assertThat(partnerRows).hasSize(2);
        assertLifecycleRows(customerRows, NotificationUserType.CUSTOMER, "INSTANT_BOOKING");
        assertLifecycleRows(partnerRows, NotificationUserType.PARTNER, "BOOKING");
        assertThat(customerRows)
                .filteredOn(notification -> "order-completed:customer:7001:IN_APP"
                        .equals(notification.getIdempotencyKey()))
                .hasSize(1);
        assertThat(partnerRows)
                .filteredOn(notification -> "order-completed:partner:7001:PUSH"
                        .equals(notification.getIdempotencyKey()))
                .hasSize(1);

        verify(fcmPushService, times(4)).sendToTokens(
                eq(List.of("customer-fcm-token")),
                any(),
                any(),
                any()
        );
        verify(fcmPushService, times(1)).sendToTokens(
                eq(List.of("partner-fcm-token")),
                any(),
                any(),
                any()
        );

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, String>> payloadCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(fcmPushService, times(5)).sendToTokens(any(), any(), any(), payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues()).anySatisfy(payload -> assertThat(payload)
                .containsEntry("userType", "CUSTOMER")
                .containsEntry("referenceType", "BOOKING")
                .containsEntry("referenceId", BOOKING_ID)
                .containsEntry("redirectType", "INSTANT_BOOKING")
                .containsEntry("redirectValue", BOOKING_ID));
        assertThat(payloadCaptor.getAllValues()).anySatisfy(payload -> assertThat(payload)
                .containsEntry("userType", "PARTNER")
                .containsEntry("referenceType", "BOOKING")
                .containsEntry("referenceId", BOOKING_ID)
                .containsEntry("redirectType", "BOOKING")
                .containsEntry("redirectValue", BOOKING_ID));

        List<NotificationLog> logs = notificationLogRepository.findAll();
        assertThat(logs).extracting(NotificationLog::getDeliveryStatus)
                .contains(DeliveryStatus.SENT, DeliveryStatus.DELIVERED);
    }

    @Test
    void scheduledBookingLifecyclePersistsScheduledRedirectRowsAndDispatchesPush() {
        triggerService.bookingCreated(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.bookingConfirmed(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.paymentSuccess(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.providerAssigned(CUSTOMER_ID, "Asha", SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.serviceStarted(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.serviceCompleted(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");
        triggerService.serviceCompleted(CUSTOMER_ID, SCHEDULED_BOOKING_ID, "SCHEDULED_BOOKING");

        List<Notification> rows =
                notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(CUSTOMER_ID);

        assertThat(rows).hasSize(12);
        assertLifecycleRows(rows, NotificationUserType.CUSTOMER, "SCHEDULED_BOOKING", SCHEDULED_BOOKING_ID);
        assertThat(rows)
                .filteredOn(notification -> ("order-completed:customer:" + SCHEDULED_BOOKING_ID + ":IN_APP")
                        .equals(notification.getIdempotencyKey()))
                .hasSize(1);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, String>> payloadCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(fcmPushService, times(6)).sendToTokens(
                eq(List.of("customer-fcm-token")),
                any(),
                any(),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getAllValues()).allSatisfy(payload -> assertThat(payload)
                .containsEntry("userType", "CUSTOMER")
                .containsEntry("referenceType", "BOOKING")
                .containsEntry("referenceId", SCHEDULED_BOOKING_ID)
                .containsEntry("redirectType", "SCHEDULED_BOOKING")
                .containsEntry("redirectValue", SCHEDULED_BOOKING_ID));
    }

    @Test
    void subscriptionReminderPersistsInAppAndPushRowsAndDispatchesPushPayload() {
        triggerService.subscriptionReminder(CUSTOMER_ID, SUBSCRIPTION_ID,
                "Your subscription visit is scheduled tomorrow.");

        List<Notification> rows =
                notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(CUSTOMER_ID);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(Notification::getChannelType)
                .containsExactlyInAnyOrder(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(rows).allSatisfy(notification -> {
            assertThat(notification.getUserType()).isEqualTo(NotificationUserType.CUSTOMER);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.SUBSCRIPTION_REMINDER);
            assertThat(notification.getReferenceType()).isEqualTo("SUBSCRIPTION");
            assertThat(notification.getReferenceId()).isEqualTo(Long.valueOf(SUBSCRIPTION_ID));
            assertThat(notification.getRedirectType()).isEqualTo("SUBSCRIPTION");
            assertThat(notification.getRedirectValue()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.isDeleted()).isFalse();
        });

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, String>> payloadCaptor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(fcmPushService).sendToTokens(
                eq(List.of("customer-fcm-token")),
                eq("Subscription Reminder"),
                eq("Your subscription visit is scheduled tomorrow."),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue())
                .containsEntry("userType", "CUSTOMER")
                .containsEntry("referenceType", "SUBSCRIPTION")
                .containsEntry("referenceId", SUBSCRIPTION_ID)
                .containsEntry("redirectType", "SUBSCRIPTION")
                .containsEntry("redirectValue", SUBSCRIPTION_ID);
    }

    private NotificationManagementService notificationManagementService(ApplicationEventPublisher eventPublisher) {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(CUSTOMER_ID));
        return new NotificationManagementService(
                notificationRepository,
                notificationLogRepository,
                mock(NotificationPreferenceRepository.class),
                mock(UserRepository.class),
                mock(LexhoUserProfileRepository.class),
                mock(AdminRepository.class),
                new NotificationMapper(),
                eventPublisher,
                securityUtils
        );
    }

    private void assertLifecycleRows(List<Notification> rows,
                                     NotificationUserType userType,
                                     String redirectType) {
        assertThat(rows).extracting(Notification::getChannelType)
                .contains(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(rows).allSatisfy(notification -> {
            assertThat(notification.getUserType()).isEqualTo(userType);
            assertThat(notification.getReferenceType()).isEqualTo("BOOKING");
            assertThat(notification.getReferenceId()).isEqualTo(Long.valueOf(BOOKING_ID));
            assertThat(notification.getRedirectType()).isEqualTo(redirectType);
            assertThat(notification.getRedirectValue()).isEqualTo(BOOKING_ID);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.isDeleted()).isFalse();
        });
    }

    private void assertLifecycleRows(List<Notification> rows,
                                     NotificationUserType userType,
                                     String redirectType,
                                     String bookingId) {
        assertThat(rows).extracting(Notification::getChannelType)
                .contains(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(rows).allSatisfy(notification -> {
            assertThat(notification.getUserType()).isEqualTo(userType);
            assertThat(notification.getReferenceType()).isEqualTo("BOOKING");
            assertThat(notification.getReferenceId()).isEqualTo(Long.valueOf(bookingId));
            assertThat(notification.getRedirectType()).isEqualTo(redirectType);
            assertThat(notification.getRedirectValue()).isEqualTo(bookingId);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.isDeleted()).isFalse();
        });
    }

    private void upsertToken(PushTokenService pushTokenService,
                             Long userId,
                             String role,
                             String deviceId,
                             String token) {
        PushTokenUpsertRequest request = new PushTokenUpsertRequest();
        request.deviceId = deviceId;
        request.fcmToken = token;
        request.platform = "ANDROID";
        request.appType = role;
        pushTokenService.upsert(userId, role, request);
    }

    private static class EventRouter implements ApplicationEventPublisher {
        private SystemNotificationEventListener systemListener;
        private NotificationDispatchListener dispatchListener;

        @Override
        public void publishEvent(ApplicationEvent event) {
            publishEvent((Object) event);
        }

        @Override
        public void publishEvent(Object event) {
            if (event instanceof SystemNotificationEvent systemEvent) {
                systemListener.onSystemEvent(systemEvent);
            } else if (event instanceof NotificationDispatchEvent dispatchEvent) {
                dispatchListener.onDispatch(dispatchEvent);
            }
        }
    }

    @Configuration
    @EntityScan(basePackageClasses = {
            Notification.class,
            NotificationLog.class,
            PushDeviceTokenEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            NotificationRepository.class,
            NotificationLogRepository.class,
            PushDeviceTokenRepository.class
    })
    static class JpaConfig {
    }
}
