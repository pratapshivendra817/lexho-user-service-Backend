package com.lexho.user_service.lexhopartner.service;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PartnerActivityFeedService {

    private static final String ACTION_REQUIRED_KEY = "actionRequired";
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String ID_KEY = "id";
    private static final String MESSAGE_KEY = "message";
    private static final String PARTNER_CATEGORY_PREFIX = "PARTNER:";
    private static final String REDIRECT_TYPE_KEY = "redirectType";
    private static final String REDIRECT_VALUE_KEY = "redirectValue";
    private static final String STATUS_KEY = "status";
    private static final String SUPPORT_TICKET_TYPE = "SUPPORT_TICKET";
    private static final String TITLE_KEY = "title";
    private static final String TYPE_KEY = "type";
    private static final String UPDATED_AT_KEY = "updatedAt";
    private static final String WARNING_TYPE = "WARNING";

    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final PartnerPenaltyRepository partnerPenaltyRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final DynamicContentTranslationService dynamicTranslationService;

    public PartnerActivityFeedService(NotificationRepository notificationRepository,
                                      SupportTicketRepository supportTicketRepository,
                                      PartnerPenaltyRepository partnerPenaltyRepository,
                                      PartnerServiceRequestRepository partnerServiceRequestRepository) {
        this(notificationRepository, supportTicketRepository, partnerPenaltyRepository, partnerServiceRequestRepository,
                Optional.empty());
    }

    public PartnerActivityFeedService(NotificationRepository notificationRepository,
                                      SupportTicketRepository supportTicketRepository,
                                      PartnerPenaltyRepository partnerPenaltyRepository,
                                      PartnerServiceRequestRepository partnerServiceRequestRepository,
                                      DynamicContentTranslationService dynamicTranslationService) {
        this(notificationRepository, supportTicketRepository, partnerPenaltyRepository, partnerServiceRequestRepository,
                Optional.ofNullable(dynamicTranslationService));
    }

    @Autowired
    public PartnerActivityFeedService(NotificationRepository notificationRepository,
                                      SupportTicketRepository supportTicketRepository,
                                      PartnerPenaltyRepository partnerPenaltyRepository,
                                      PartnerServiceRequestRepository partnerServiceRequestRepository,
                                      Optional<DynamicContentTranslationService> dynamicTranslationService) {
        this.notificationRepository = notificationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.partnerPenaltyRepository = partnerPenaltyRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.dynamicTranslationService = optionalValue(dynamicTranslationService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    public PageResponseDto<Map<String, Object>> activity(Long partnerId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> items = new ArrayList<>();
        notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(partnerId)
                .stream()
                .filter(notification -> notification.getChannelType() == com.lexho.notification.enums.ChannelType.IN_APP)
                .limit(100)
                .map(this::notificationItem)
                .forEach(items::add);
        supportTicketRepository.findByUserIdAndCategoryIdStartingWithOrderByCreatedAtDesc(
                        partnerId,
                        PARTNER_CATEGORY_PREFIX,
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(this::supportItem)
                .forEach(items::add);
        partnerPenaltyRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .filter(penalty -> WARNING_TYPE.equalsIgnoreCase(penalty.getStatus()))
                .limit(100)
                .map(this::warningItem)
                .forEach(items::add);
        partnerServiceRequestRepository.findByPartnerIdOrderByCreatedAtDesc(
                        partnerId,
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(this::dispatchItem)
                .forEach(items::add);

        items.sort(Comparator.comparing(this::itemDate, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = items.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) safeSize);
        return new PageResponseDto<>(items.subList(fromIndex, toIndex), safePage, safeSize, total, totalPages);
    }

    private Map<String, Object> notificationItem(Notification notification) {
        Map<String, Object> item = baseItem("NOTIFICATION", String.valueOf(notification.getId()), notification.getCreatedAt(), notification.getUpdatedAt());
        item.put(TITLE_KEY, notification.getTitle());
        item.put(MESSAGE_KEY, notification.getMessage());
        item.put(STATUS_KEY, notification.getStatus() == null ? null : notification.getStatus().name());
        item.put(REDIRECT_TYPE_KEY, notification.getRedirectType());
        item.put(REDIRECT_VALUE_KEY, notification.getRedirectValue());
        item.put("referenceType", notification.getReferenceType());
        item.put("referenceId", notification.getReferenceId());
        item.put(ACTION_REQUIRED_KEY, notification.getReadAt() == null);
        return item;
    }

    private Map<String, Object> supportItem(SupportTicket ticket) {
        Map<String, Object> item = baseItem(SUPPORT_TICKET_TYPE, "TKT-" + String.format("%06d", ticket.getId()), ticket.getCreatedAt(), ticket.getUpdatedAt());
        item.put(TITLE_KEY, ticket.getSubject());
        item.put(MESSAGE_KEY, ticket.getMessage());
        item.put(STATUS_KEY, ticket.getStatus());
        item.put(REDIRECT_TYPE_KEY, SUPPORT_TICKET_TYPE);
        item.put(REDIRECT_VALUE_KEY, "TKT-" + String.format("%06d", ticket.getId()));
        item.put(ACTION_REQUIRED_KEY, !"CLOSED".equalsIgnoreCase(ticket.getStatus()) && !"RESOLVED".equalsIgnoreCase(ticket.getStatus()));
        return item;
    }

    private Map<String, Object> warningItem(PartnerPenalty warning) {
        Map<String, Object> item = baseItem(WARNING_TYPE, String.valueOf(warning.getId()), warning.getCreatedAt(), warning.getUpdatedAt());
        item.put(TITLE_KEY, WARNING_TYPE);
        item.put(MESSAGE_KEY, translate(warning.getPartnerId(), "partner_warning", warning.getId(),
                "reason", warning.getReason() == null ? "" : warning.getReason()));
        item.put(STATUS_KEY, warning.getStatus());
        item.put(REDIRECT_TYPE_KEY, WARNING_TYPE);
        item.put(REDIRECT_VALUE_KEY, String.valueOf(warning.getId()));
        item.put(ACTION_REQUIRED_KEY, !"RESOLVED".equalsIgnoreCase(warning.getStatus()));
        item.put("disputeStatus", warning.getDisputeStatus());
        return item;
    }

    private Map<String, Object> dispatchItem(PartnerServiceRequestEntity request) {
        Map<String, Object> item = baseItem("DISPATCH_REQUEST", String.valueOf(request.getId()), request.getCreatedAt(), request.getCreatedAt());
        item.put(TITLE_KEY, "Dispatch Request");
        item.put(MESSAGE_KEY, request.getServiceType() == null
                ? "New service request"
                : translate(request.getPartnerId(), "partner_service_request", request.getId(),
                "service_type", request.getServiceType()));
        item.put(STATUS_KEY, request.getStatus() == null ? null : request.getStatus().name());
        item.put(REDIRECT_TYPE_KEY, "DISPATCH_REQUEST");
        item.put(REDIRECT_VALUE_KEY, String.valueOf(request.getId()));
        item.put(ACTION_REQUIRED_KEY, request.getStatus() != null && "PENDING".equalsIgnoreCase(request.getStatus().name()));
        item.put("bookingAddress", request.getFullAddress());
        item.put("scheduledTime", request.getScheduledTime());
        return item;
    }

    private String translate(Long partnerId, String entityType, Object entityId, String fieldName, String englishText) {
        if (dynamicTranslationService == null || englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return dynamicTranslationService.translateForPartner(partnerId, null, entityType, entityId, fieldName, englishText);
    }

    private Map<String, Object> baseItem(String type, String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put(TYPE_KEY, type);
        item.put(ID_KEY, id);
        item.put(CREATED_AT_KEY, createdAt);
        item.put(UPDATED_AT_KEY, updatedAt);
        return item;
    }

    private LocalDateTime itemDate(Map<String, Object> item) {
        Object updatedAt = item.get(UPDATED_AT_KEY);
        if (updatedAt instanceof LocalDateTime value) {
            return value;
        }
        Object createdAt = item.get(CREATED_AT_KEY);
        return createdAt instanceof LocalDateTime value ? value : null;
    }
}
