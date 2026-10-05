package com.lexho.user_service.admin.service;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.admin.entity.ActivityLog;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.ActivityLogRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminActivityFeedService {

    private static final String CREATED_AT_KEY = "createdAt";
    private static final String ID_KEY = "id";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_KEY = "status";
    private static final String TYPE_KEY = "type";
    private static final String UPDATED_AT_KEY = "updatedAt";
    private static final String NOTIFICATION_TYPE = "NOTIFICATION";
    private static final String SUPPORT_TICKET_TYPE = "SUPPORT_TICKET";

    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ActivityLogRepository activityLogRepository;

    public AdminActivityFeedService(NotificationRepository notificationRepository,
                                    SupportTicketRepository supportTicketRepository,
                                    ActivityLogRepository activityLogRepository) {
        this.notificationRepository = notificationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public Map<String, Object> activity(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int sampleSize = Math.max(safeLimit * 3, 20);

        List<Map<String, Object>> items = new ArrayList<>();
        notificationRepository.findAll(PageRequest.of(0, sampleSize, Sort.by(Sort.Direction.DESC, CREATED_AT_KEY)))
                .stream()
                .filter(notification -> !notification.isDeleted() && notification.getChannelType() == ChannelType.IN_APP)
                .map(this::mapNotification)
                .forEach(items::add);
        supportTicketRepository.findAll(PageRequest.of(0, sampleSize, Sort.by(Sort.Direction.DESC, UPDATED_AT_KEY)))
                .stream()
                .map(this::mapSupportTicket)
                .forEach(items::add);
        activityLogRepository.findAll(PageRequest.of(0, sampleSize, Sort.by(Sort.Direction.DESC, CREATED_AT_KEY)))
                .stream()
                .map(this::mapActivityLog)
                .forEach(items::add);

        items.sort(Comparator.comparing(this::activityTime, Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.min((safePage - 1) * safeLimit, items.size());
        int toIndex = Math.min(fromIndex + safeLimit, items.size());
        List<Map<String, Object>> paged = items.subList(fromIndex, toIndex);

        return Map.of(
                "data", paged,
                "meta", Map.of(
                        "page", safePage,
                        "limit", safeLimit,
                        "total", items.size(),
                        "hasMore", toIndex < items.size()
                )
        );
    }

    private Map<String, Object> mapNotification(Notification notification) {
        Map<String, Object> item = new HashMap<>();
        item.put(TYPE_KEY, NOTIFICATION_TYPE);
        item.put(ID_KEY, "NTF-" + notification.getId());
        item.put("title", notification.getTitle());
        item.put(MESSAGE_KEY, notification.getMessage());
        item.put(STATUS_KEY, notification.getStatus() == null ? null : notification.getStatus().name());
        item.put("userId", notification.getUserId());
        item.put("userType", notification.getUserType() == null ? null : notification.getUserType().name());
        item.put("notificationType", notification.getNotificationType() == null ? null : notification.getNotificationType().name());
        item.put("redirectType", notification.getRedirectType());
        item.put("redirectValue", notification.getRedirectValue());
        item.put(CREATED_AT_KEY, notification.getCreatedAt());
        item.put(UPDATED_AT_KEY, notification.getUpdatedAt());
        return item;
    }

    private Map<String, Object> mapSupportTicket(SupportTicket ticket) {
        Map<String, Object> item = new HashMap<>();
        item.put(TYPE_KEY, SUPPORT_TICKET_TYPE);
        item.put(ID_KEY, "TCK-" + (10000 + ticket.getId()));
        item.put("subject", ticket.getSubject());
        item.put(MESSAGE_KEY, ticket.getMessage());
        item.put(STATUS_KEY, ticket.getStatus());
        item.put("requesterType", isPartnerTicket(ticket) ? "PARTNER" : "CUSTOMER");
        item.put("userId", ticket.getUserId());
        item.put("priority", ticket.getPriority());
        item.put("categoryId", ticket.getCategoryId());
        item.put("bookingId", ticket.getBookingId());
        item.put(CREATED_AT_KEY, ticket.getCreatedAt());
        item.put(UPDATED_AT_KEY, ticket.getUpdatedAt());
        return item;
    }

    private Map<String, Object> mapActivityLog(ActivityLog log) {
        Map<String, Object> item = new HashMap<>();
        item.put(TYPE_KEY, "AUDIT_LOG");
        item.put(ID_KEY, "LOG-" + log.getId());
        item.put("adminId", log.getAdminId());
        item.put("action", log.getAction());
        item.put("entity", log.getEntity());
        item.put("entityId", log.getEntityId());
        item.put("metaData", log.getMetaData());
        item.put(CREATED_AT_KEY, log.getCreatedAt());
        item.put(UPDATED_AT_KEY, log.getCreatedAt());
        return item;
    }

    private LocalDateTime activityTime(Map<String, Object> item) {
        Object updatedAt = item.get(UPDATED_AT_KEY);
        if (updatedAt instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        Object createdAt = item.get(CREATED_AT_KEY);
        return createdAt instanceof LocalDateTime localDateTime ? localDateTime : null;
    }

    private boolean isPartnerTicket(SupportTicket ticket) {
        return ticket.getCategoryId() != null && ticket.getCategoryId().startsWith("PARTNER:");
    }
}
