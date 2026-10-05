package com.lexho.user_service.customer.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.notification.enums.ChannelType;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerActivityFeedService {

    private static final String ACTION_REQUIRED_KEY = "actionRequired";
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String ID_KEY = "id";
    private static final String MESSAGE_KEY = "message";
    private static final String REDIRECT_TYPE_KEY = "redirectType";
    private static final String REDIRECT_VALUE_KEY = "redirectValue";
    private static final String STATUS_KEY = "status";
    private static final String SUPPORT_TICKET_TYPE = "SUPPORT_TICKET";
    private static final String TITLE_KEY = "title";
    private static final String TYPE_KEY = "type";
    private static final String UPDATED_AT_KEY = "updatedAt";

    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final OrderRepository orderRepository;

    public CustomerActivityFeedService(NotificationRepository notificationRepository,
                                       SupportTicketRepository supportTicketRepository,
                                       OrderRepository orderRepository) {
        this.notificationRepository = notificationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> activity(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> items = new ArrayList<>();

        notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).stream()
                .filter(notification -> notification.getChannelType() == ChannelType.IN_APP)
                .limit(100)
                .map(this::notificationItem)
                .forEach(items::add);

        supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)).stream()
                .map(this::supportItem)
                .forEach(items::add);

        orderRepository.findByUserId(userId).stream()
                .limit(100)
                .map(this::bookingItem)
                .forEach(items::add);

        items.sort(Comparator.comparing(this::activityDate, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = items.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        return Map.of(
                "data", items.subList(fromIndex, toIndex),
                "meta", Map.of(
                        "page", safePage,
                        "limit", safeSize,
                        "total", total,
                        "totalPages", total == 0 ? 0 : (int) Math.ceil(total / (double) safeSize)
                )
        );
    }

    private Map<String, Object> notificationItem(Notification notification) {
        Map<String, Object> row = baseItem("NOTIFICATION", String.valueOf(notification.getId()),
                notification.getCreatedAt(), notification.getUpdatedAt());
        row.put(TITLE_KEY, notification.getTitle());
        row.put(MESSAGE_KEY, notification.getMessage());
        row.put(STATUS_KEY, notification.getStatus() == null ? null : notification.getStatus().name());
        row.put(REDIRECT_TYPE_KEY, notification.getRedirectType());
        row.put(REDIRECT_VALUE_KEY, notification.getRedirectValue());
        row.put("referenceType", notification.getReferenceType());
        row.put("referenceId", notification.getReferenceId());
        row.put(ACTION_REQUIRED_KEY, notification.getReadAt() == null);
        return row;
    }

    private Map<String, Object> supportItem(SupportTicket ticket) {
        Map<String, Object> row = baseItem(SUPPORT_TICKET_TYPE, formatTicketId(ticket.getId()),
                ticket.getCreatedAt(), ticket.getUpdatedAt());
        row.put(TITLE_KEY, ticket.getSubject());
        row.put(MESSAGE_KEY, ticket.getMessage());
        row.put(STATUS_KEY, ticket.getStatus());
        row.put(REDIRECT_TYPE_KEY, SUPPORT_TICKET_TYPE);
        row.put(REDIRECT_VALUE_KEY, formatTicketId(ticket.getId()));
        row.put(ACTION_REQUIRED_KEY, !"CLOSED".equalsIgnoreCase(ticket.getStatus()) && !"RESOLVED".equalsIgnoreCase(ticket.getStatus()));
        return row;
    }

    private Map<String, Object> bookingItem(Order order) {
        Map<String, Object> row = baseItem("BOOKING", String.valueOf(order.getId()),
                order.getCreatedAt(), order.getUpdatedAt());
        row.put(TITLE_KEY, order.getBookingType() == null ? "Booking" : order.getBookingType() + " booking");
        row.put(MESSAGE_KEY, order.getAddress() == null ? "" : order.getAddress());
        row.put(STATUS_KEY, order.getStatus() == null ? null : order.getStatus().name());
        row.put(REDIRECT_TYPE_KEY, "BOOKING");
        row.put(REDIRECT_VALUE_KEY, String.valueOf(order.getId()));
        row.put(ACTION_REQUIRED_KEY, order.getStatus() != null && !isTerminal(order.getStatus().name()));
        row.put("bookingType", order.getBookingType());
        row.put("scheduledAt", order.getScheduledAt());
        return row;
    }

    private Map<String, Object> baseItem(String type, String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(TYPE_KEY, type);
        row.put(ID_KEY, id);
        row.put(CREATED_AT_KEY, createdAt);
        row.put(UPDATED_AT_KEY, updatedAt);
        return row;
    }

    private LocalDateTime activityDate(Map<String, Object> row) {
        Object updatedAt = row.get(UPDATED_AT_KEY);
        if (updatedAt instanceof LocalDateTime value) {
            return value;
        }
        Object createdAt = row.get(CREATED_AT_KEY);
        return createdAt instanceof LocalDateTime value ? value : null;
    }

    private boolean isTerminal(String status) {
        return "CANCELLED".equalsIgnoreCase(status)
                || "SERVICE_COMPLETED".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status)
                || "PAYMENT_COMPLETED".equalsIgnoreCase(status)
                || "INSTANT_FAILED".equalsIgnoreCase(status);
    }

    private String formatTicketId(Long id) {
        return id == null ? "" : "TCK-" + (10000 + id);
    }
}
