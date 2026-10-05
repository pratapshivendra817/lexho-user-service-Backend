package com.lexho.user_service.integration;

import com.lexho.testconfig.LexhoInfrastructureTest;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.entity.NotificationLog;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationLogRepository;
import com.lexho.notification.repository.NotificationPreferenceRepository;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.lexhopartner.controller.AdminPartnerControlController;
import com.lexho.user_service.lexhopartner.controller.LexhoPartnerController;
import com.lexho.user_service.lexhopartner.controller.LexhoPartnerNotificationController;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerServiceAreaRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.lexhopartner.service.PartnerActivityFeedService;
import com.lexho.user_service.lexhopartner.service.PartnerCatalogService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationsService;
import com.lexho.user_service.lexhopartner.service.PartnerWarningRealtimeService;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.notification.service.NotificationRealtimeService;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@LexhoInfrastructureTest
@ContextConfiguration(classes = PartnerWarningNotificationFlowIT.JpaConfig.class)
@ActiveProfiles("it")
class PartnerWarningNotificationFlowIT {

    private static final Long PARTNER_ID = 501L;
    private static final Long ADMIN_ID = 9L;

    @Autowired
    private PartnerPenaltyRepository partnerPenaltyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private TestEntityManager entityManager;

    private MockMvc adminMockMvc;
    private MockMvc partnerMockMvc;
    private MockMvc notificationMockMvc;
    private AuditLogService auditLogService;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(PARTNER_ID));

        NotificationMapper notificationMapper = new NotificationMapper();
        NotificationManagementService notificationManagementService = new NotificationManagementService(
                notificationRepository,
                notificationLogRepository,
                mock(NotificationPreferenceRepository.class),
                mock(UserRepository.class),
                mock(LexhoUserProfileRepository.class),
                mock(AdminRepository.class),
                notificationMapper,
                eventPublisher,
                securityUtils
        );
        PartnerOperationsService partnerOperationsService = new PartnerOperationsService(
                mock(PartnerLeaveRepository.class),
                mock(PartnerServiceAreaRepository.class),
                partnerPenaltyRepository,
                mock(PartnerWarningCommentRepository.class),
                mock(PartnerOrderRepository.class),
                mock(PartnerOperationalProfileRepository.class),
                mock(PartnerShiftRepository.class),
                mock(ReviewRepository.class),
                auditLogService,
                notificationManagementService,
                mock(PartnerWarningRealtimeService.class)
        );

        AdminPartnerControlController adminController = new AdminPartnerControlController(
                null, null, null, null, partnerOperationsService);
        LexhoPartnerController partnerController = new LexhoPartnerController(
                null, null, null, null, null, partnerOperationsService, mock(PartnerActivityFeedService.class),
                mock(PartnerCatalogService.class), mock(FileStorageUtil.class));
        LexhoPartnerNotificationController notificationController =
                new LexhoPartnerNotificationController(notificationManagementService, notificationMapper,
                        mock(NotificationRealtimeService.class));

        adminMockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        partnerMockMvc = MockMvcBuilders.standaloneSetup(partnerController).build();
        notificationMockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    void adminWarningCreatePersistsWarningAndCreatesPartnerNotificationFlow() throws Exception {
        adminMockMvc.perform(post("/api/v1/admin/partners/{partnerId}/warnings", PARTNER_ID)
                        .requestAttr("userId", ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": " Document compliance issue ",
                                  "sendNotification": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.warningId").isNumber())
                .andExpect(jsonPath("$.data.type").value("WARNING"))
                .andExpect(jsonPath("$.data.reason").value("Document compliance issue"))
                .andExpect(jsonPath("$.data.redirectType").value("WARNING"));

        entityManager.flush();
        entityManager.clear();

        PartnerPenalty warning = partnerPenaltyRepository.findByPartnerIdOrderByCreatedAtDesc(PARTNER_ID)
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(warning.getStatus()).isEqualTo("WARNING");
        assertThat(warning.getReason()).isEqualTo("Document compliance issue");
        assertThat(warning.getCreatedByAdminId()).isEqualTo(ADMIN_ID);

        partnerMockMvc.perform(get("/api/v1/partner/warnings")
                        .requestAttr("userId", PARTNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(warning.getId()))
                .andExpect(jsonPath("$.data[0].warningId").value(warning.getId()))
                .andExpect(jsonPath("$.data[0].category").value("WARNING"))
                .andExpect(jsonPath("$.data[0].notificationTitle").value("New Warning Issued"))
                .andExpect(jsonPath("$.data[0].notificationMessage")
                        .value("A new warning has been issued to your account. Tap to view the details."))
                .andExpect(jsonPath("$.data[0].redirectValue").value(String.valueOf(warning.getId())));

        List<Notification> notifications = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(PARTNER_ID);
        assertThat(notifications).hasSize(2);
        assertThat(notifications)
                .extracting(Notification::getChannelType)
                .containsExactlyInAnyOrder(ChannelType.IN_APP, ChannelType.PUSH);
        assertThat(notifications).allSatisfy(notification -> {
            assertThat(notification.getUserType()).isEqualTo(NotificationUserType.PARTNER);
            assertThat(notification.getReferenceType()).isEqualTo("WARNING");
            assertThat(notification.getReferenceId()).isEqualTo(warning.getId());
            assertThat(notification.getRedirectType()).isEqualTo("WARNING");
            assertThat(notification.getRedirectValue()).isEqualTo(String.valueOf(warning.getId()));
        });

        Notification inApp = notifications.stream()
                .filter(notification -> notification.getChannelType() == ChannelType.IN_APP)
                .max(Comparator.comparing(Notification::getId))
                .orElseThrow();
        notificationMockMvc.perform(get("/api/v1/partner/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(inApp.getId()))
                .andExpect(jsonPath("$.data.content[0].redirectType").value("WARNING"))
                .andExpect(jsonPath("$.data.content[0].redirectValue").value(String.valueOf(warning.getId())));

        notificationMockMvc.perform(get("/api/v1/partner/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        notificationMockMvc.perform(patch("/api/v1/partner/notifications/{notificationId}/read", inApp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ"));

        notificationMockMvc.perform(get("/api/v1/partner/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        verify(auditLogService).log(ADMIN_ID, "ADMIN", "WARNING_CREATED", "PARTNER",
                String.valueOf(PARTNER_ID), null, "WARNING");
        verify(eventPublisher, times(2)).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Configuration
    @EntityScan(basePackageClasses = {PartnerPenalty.class, Notification.class, NotificationLog.class})
    @EnableJpaRepositories(basePackageClasses = {
            PartnerPenaltyRepository.class,
            NotificationRepository.class,
            NotificationLogRepository.class
    })
    static class JpaConfig {
    }
}
