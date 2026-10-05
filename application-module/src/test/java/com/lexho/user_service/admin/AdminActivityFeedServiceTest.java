package com.lexho.user_service.admin;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.admin.entity.ActivityLog;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.ActivityLogRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.admin.service.AdminActivityFeedService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminActivityFeedServiceTest {

    @Test
    void activityMergesAndSortsNotificationsSupportAndAuditLogs() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        SupportTicketRepository supportTicketRepository = mock(SupportTicketRepository.class);
        ActivityLogRepository activityLogRepository = mock(ActivityLogRepository.class);
        AdminActivityFeedService service = new AdminActivityFeedService(
                notificationRepository,
                supportTicketRepository,
                activityLogRepository);

        Notification notification = new Notification();
        notification.setUserId(7L);
        notification.setUserType(NotificationUserType.CUSTOMER);
        notification.setTitle("Dispatch alert");
        notification.setMessage("Partner found");
        notification.setNotificationType(NotificationType.SYSTEM);
        notification.setChannelType(ChannelType.IN_APP);
        notification.setPriority(NotificationPriority.MEDIUM);
        notification.setStatus(NotificationStatus.SENT);
        ReflectionTestUtils.setField(notification, "id", 11L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 7, 27, 12, 0));
        ReflectionTestUtils.setField(notification, "updatedAt", LocalDateTime.of(2026, 7, 27, 12, 0));

        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(8L);
        ticket.setSubject("Refund");
        ticket.setMessage("Need refund");
        ticket.setStatus("OPEN");
        ticket.setCategoryId("PAYMENT");
        ReflectionTestUtils.setField(ticket, "id", 12L);
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.of(2026, 7, 27, 13, 0));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.of(2026, 7, 27, 13, 30));

        ActivityLog log = new ActivityLog();
        ReflectionTestUtils.setField(log, "id", 13L);
        log.setAdminId(2L);
        log.setAction("SUPPORT_REPLY");
        log.setEntity("support_tickets");
        log.setEntityId(12L);
        log.setMetaData("status=IN_PROGRESS");
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 7, 27, 11, 0));

        when(notificationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(supportTicketRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));
        when(activityLogRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Map<String, Object> result = service.activity(1, 10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) result.get("meta");

        assertThat(data).hasSize(3);
        assertThat(data.get(0)).containsEntry("type", "SUPPORT_TICKET");
        assertThat(data.get(1)).containsEntry("type", "NOTIFICATION");
        assertThat(data.get(2)).containsEntry("type", "AUDIT_LOG");
        assertThat(meta).containsEntry("page", 1).containsEntry("limit", 10).containsEntry("total", 3);
    }
}
