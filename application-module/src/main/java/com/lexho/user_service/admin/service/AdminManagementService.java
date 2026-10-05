package com.lexho.user_service.admin.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.entity.*;
import com.lexho.user_service.admin.repository.*;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.support.service.SupportRealtimeService;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminManagementService {

    private static final String ID_KEY = "id";
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String UPDATED_AT_KEY = "updatedAt";
    private static final String PAYMENT_STATUS_KEY = "paymentStatus";
    private static final String BOOKING_ID_KEY = "bookingId";
    private static final String AMOUNT_KEY = "amount";
    private static final String NAME_KEY = "name";
    private static final String IMAGE_URL_KEY = "imageUrl";
    private static final String ATTACHMENT_URLS_KEY = "attachmentUrls";
    private static final String SENDER_KEY = "sender";
    private static final String SUPPORT_TICKETS_ENTITY = "support_tickets";
    private static final String SUPPORT_SENDER_TYPE = "SUPPORT";
    private static final String CUSTOMER_REQUESTER_TYPE = "CUSTOMER";
    private static final String PARTNER_REQUESTER_TYPE = "PARTNER";
    private static final String PARTNER_CATEGORY_PREFIX = "PARTNER:";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String SUPPORT_TICKET_NOT_FOUND_MESSAGE = "Support ticket not found";
    private static final String TICKET_CODE_PREFIX = "TCK-";
    private static final Set<String> SUPPORT_TICKET_STATUSES = Set.of("OPEN", STATUS_IN_PROGRESS, "RESOLVED",
            STATUS_CLOSED);

    private final OrderRepository orderRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final BannerRepository bannerRepository;
    private final PayoutRepository payoutRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketReplyRepository supportTicketReplyRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;
    private final SupportRealtimeService supportRealtimeService;

    public AdminManagementService(PartnerRepository partnerRepository,
                                  OrderRepository orderRepository,
                                  AdminServiceEntityRepository adminServiceEntityRepository,
                                  BannerRepository bannerRepository,
                                  PayoutRepository payoutRepository,
                                  ActivityLogRepository activityLogRepository,
                                  SupportTicketRepository supportTicketRepository,
                                  SupportTicketReplyRepository supportTicketReplyRepository,
                                  AdminAuditService adminAuditService,
                                  SecurityUtils securityUtils) {
        this(partnerRepository, orderRepository, adminServiceEntityRepository, bannerRepository, payoutRepository,
                activityLogRepository, supportTicketRepository, supportTicketReplyRepository, adminAuditService,
                securityUtils, Optional.empty());
    }

    @Autowired
    public AdminManagementService(PartnerRepository partnerRepository,
                                  OrderRepository orderRepository,
                                  AdminServiceEntityRepository adminServiceEntityRepository,
                                  BannerRepository bannerRepository,
                                  PayoutRepository payoutRepository,
                                  ActivityLogRepository activityLogRepository,
                                  SupportTicketRepository supportTicketRepository,
                                  SupportTicketReplyRepository supportTicketReplyRepository,
                                  AdminAuditService adminAuditService,
                                  SecurityUtils securityUtils,
                                  Optional<SupportRealtimeService> supportRealtimeService) {
        this.orderRepository = orderRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.bannerRepository = bannerRepository;
        this.payoutRepository = payoutRepository;
        this.activityLogRepository = activityLogRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.supportTicketReplyRepository = supportTicketReplyRepository;
        this.adminAuditService = adminAuditService;
        this.securityUtils = securityUtils;
        this.supportRealtimeService = supportRealtimeService.orElse(null);
    }

    public PagedDataResponse<Map<String, Object>> bookings(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, ID_KEY);
        OrderStatus orderStatus = parseOrderStatus(status);
        Page<Order> result = orderRepository.searchOrders(normalize(search), orderStatus, pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapOrder).collect(Collectors.toList()), result.getTotalElements());
    }

    public DataResponse<Map<String, Object>> bookingById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return new DataResponse<>(mapOrder(order));
    }

    public PagedDataResponse<Map<String, Object>> services(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, CREATED_AT_KEY);
        Page<AdminServiceEntity> result = adminServiceEntityRepository.search(normalize(search), normalize(status), pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapService).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> banners(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, CREATED_AT_KEY);
        Boolean active = parseActiveStatus(status).orElse(null);
        Page<Banner> result = bannerRepository.search(normalize(search), active, pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapBanner).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> payouts(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, CREATED_AT_KEY);
        Page<Payout> result = payoutRepository.search(normalize(search), normalize(status), pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapPayout).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> activityLogs(int page, int limit, String search) {
        Pageable pageable = page(page, limit, CREATED_AT_KEY);
        Page<ActivityLog> result = activityLogRepository.search(normalize(search), pageable);
        List<Map<String, Object>> items = result.getContent().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put(ID_KEY, log.getId());
            map.put("adminId", log.getAdminId());
            map.put("action", log.getAction());
            map.put("entity", log.getEntity());
            map.put("entityId", log.getEntityId());
            map.put("metaData", log.getMetaData());
            map.put(CREATED_AT_KEY, log.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return new PagedDataResponse<>(items, result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> supportTickets(int page,
                                                                 int limit,
                                                                 String search,
                                                                 String status,
                                                                 String categoryId,
                                                                 String priority) {
        Pageable pageable = page(page, limit, UPDATED_AT_KEY);
        Page<SupportTicket> result = supportTicketRepository.search(
                normalize(search),
                normalize(status),
                normalize(categoryId),
                normalize(priority),
                pageable
        );
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapTicket).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> customerSupportTickets(int page,
                                                                         int limit,
                                                                         String search,
                                                                         String status,
                                                                         String categoryId,
                                                                         String priority) {
        return supportTicketsByRequesterType(page, limit, search, status, categoryId, priority, false);
    }

    public PagedDataResponse<Map<String, Object>> partnerSupportTickets(int page,
                                                                        int limit,
                                                                        String search,
                                                                        String status,
                                                                        String categoryId,
                                                                        String priority) {
        return supportTicketsByRequesterType(page, limit, search, status, categoryId, priority, true);
    }

    private PagedDataResponse<Map<String, Object>> supportTicketsByRequesterType(int page,
                                                                                 int limit,
                                                                                 String search,
                                                                                 String status,
                                                                                 String categoryId,
                                                                                 String priority,
                                                                                 boolean partnerTickets) {
        Pageable pageable = page(page, limit, UPDATED_AT_KEY);
        Page<SupportTicket> result = supportTicketRepository.searchByRequesterType(
                normalize(search),
                normalize(status),
                normalize(categoryId),
                normalize(priority),
                partnerTickets,
                pageable
        );
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapTicket).collect(Collectors.toList()), result.getTotalElements());
    }

    public DataResponse<Map<String, Object>> supportTicketDetail(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPORT_TICKET_NOT_FOUND_MESSAGE));
        return new DataResponse<>(mapTicket(ticket));
    }

    public SseEmitter supportTicketStream(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPORT_TICKET_NOT_FOUND_MESSAGE));
        if (supportRealtimeService == null) {
            throw new BadRequestException("Support realtime stream is not available");
        }
        return supportRealtimeService.subscribeAdmin(ticket.getId());
    }

    @Transactional
    public DataResponse<Map<String, Object>> updateSupportTicketStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORT_TICKET_STATUSES.contains(normalized)) {
            throw new BadRequestException("Status must be OPEN, IN_PROGRESS, RESOLVED or CLOSED");
        }
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPORT_TICKET_NOT_FOUND_MESSAGE));
        ticket.setStatus(normalized);
        supportTicketRepository.save(ticket);
        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put(ID_KEY, TICKET_CODE_PREFIX + (10000 + ticket.getId()));
        statusPayload.put(STATUS_KEY, ticket.getStatus());
        statusPayload.put(UPDATED_AT_KEY, String.valueOf(ticket.getUpdatedAt()));
        publishSupportEvent(ticket, "ticket-status", statusPayload);
        audit("SUPPORT_STATUS_UPDATE", SUPPORT_TICKETS_ENTITY, ticket.getId(), "status=" + ticket.getStatus());
        return new DataResponse<>(mapTicket(ticket));
    }

    @Transactional
    public DataResponse<Map<String, Object>> replySupportTicket(Long id, String message) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Reply message is required");
        }
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPORT_TICKET_NOT_FOUND_MESSAGE));
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(currentAdminId());
        reply.setSenderType(SUPPORT_SENDER_TYPE);
        reply.setMessage(message.trim());
        supportTicketReplyRepository.save(reply);

        if (!STATUS_CLOSED.equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus(STATUS_IN_PROGRESS);
            supportTicketRepository.save(ticket);
        }
        Map<String, Object> messagePayload = new HashMap<>();
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put(SENDER_KEY, SUPPORT_SENDER_TYPE);
        messageBody.put("text", reply.getMessage());
        messageBody.put(ATTACHMENT_URLS_KEY, deserializeAttachmentUrls(reply.getAttachmentUrls()));
        messageBody.put(CREATED_AT_KEY, String.valueOf(reply.getCreatedAt()));
        messagePayload.put("ticketId", TICKET_CODE_PREFIX + (10000 + ticket.getId()));
        messagePayload.put(STATUS_KEY, ticket.getStatus());
        messagePayload.put(MESSAGE_KEY, messageBody);
        publishSupportEvent(ticket, "ticket-message", messagePayload);
        audit("SUPPORT_REPLY", SUPPORT_TICKETS_ENTITY, ticket.getId(), null);
        return new DataResponse<>(mapTicket(ticket));
    }

    private Pageable page(int page, int limit, String sortBy) {
        int safePage = Math.max(page, 1) - 1;
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, sortBy));
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }

    private Optional<Boolean> parseActiveStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return Optional.empty();
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(normalized)) {
            return Optional.of(true);
        }
        if (STATUS_INACTIVE.equalsIgnoreCase(normalized)) {
            return Optional.of(false);
        }
        throw new BadRequestException("Invalid status filter");
    }

    private OrderStatus parseOrderStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid booking status filter");
        }
    }

    private Map<String, Object> mapOrder(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID_KEY, order.getId());
        map.put("userId", order.getUserId());
        map.put("totalAmount", order.getTotalAmount());
        map.put(STATUS_KEY, order.getStatus());
        map.put(PAYMENT_STATUS_KEY, order.getPaymentStatus());
        map.put("address", order.getAddress());
        return map;
    }

    private Map<String, Object> mapService(AdminServiceEntity service) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID_KEY, service.getId());
        map.put(NAME_KEY, service.getName());
        map.put(STATUS_KEY, service.getStatus());
        map.put("categoryName", service.getCategoryName());
        map.put(CREATED_AT_KEY, service.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapBanner(Banner banner) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID_KEY, banner.getId());
        map.put("title", banner.getTitle());
        map.put(IMAGE_URL_KEY, banner.getImageUrl());
        map.put("active", banner.isActive());
        map.put(CREATED_AT_KEY, banner.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapPayout(Payout payout) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID_KEY, payout.getId());
        map.put("providerId", payout.getProviderId());
        map.put(AMOUNT_KEY, payout.getAmount());
        map.put(STATUS_KEY, payout.getStatus());
        map.put(CREATED_AT_KEY, payout.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapTicket(SupportTicket ticket) {
        Map<String, Object> map = new HashMap<>();
        boolean partnerTicket = isPartnerTicket(ticket);
        map.put(ID_KEY, ticket.getId());
        map.put("userId", ticket.getUserId());
        map.put("requesterType", partnerTicket ? PARTNER_REQUESTER_TYPE : CUSTOMER_REQUESTER_TYPE);
        map.put("subject", ticket.getSubject());
        map.put(MESSAGE_KEY, ticket.getMessage());
        map.put(STATUS_KEY, ticket.getStatus());
        map.put("categoryId", displayCategoryId(ticket.getCategoryId()));
        map.put("rawCategoryId", ticket.getCategoryId());
        map.put(BOOKING_ID_KEY, ticket.getBookingId());
        map.put("requesterName", ticket.getRequesterName());
        map.put("requesterEmail", ticket.getRequesterEmail());
        map.put("requesterPhone", ticket.getRequesterPhone());
        map.put("priority", ticket.getPriority());
        map.put(ATTACHMENT_URLS_KEY, deserializeAttachmentUrls(ticket.getAttachmentUrls()));
        map.put("ticketId", TICKET_CODE_PREFIX + (10000 + ticket.getId()));
        map.put(CREATED_AT_KEY, ticket.getCreatedAt());
        map.put(UPDATED_AT_KEY, ticket.getUpdatedAt());
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                ID_KEY, "MSG-1",
                SENDER_KEY, partnerTicket ? PARTNER_REQUESTER_TYPE : CUSTOMER_REQUESTER_TYPE,
                MESSAGE_KEY, ticket.getMessage(),
                ATTACHMENT_URLS_KEY, deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                CREATED_AT_KEY, ticket.getCreatedAt()
        ));
        List<SupportTicketReply> replies = supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(Map.of(
                    ID_KEY, "MSG-" + (i + 2),
                    SENDER_KEY, normalizeReplySender(reply),
                    "adminId", reply.getAdminId(),
                    MESSAGE_KEY, reply.getMessage(),
                    ATTACHMENT_URLS_KEY, deserializeAttachmentUrls(reply.getAttachmentUrls()),
                    CREATED_AT_KEY, reply.getCreatedAt()
            ));
        }
        map.put("messages", messages);
        return map;
    }

    private List<String> deserializeAttachmentUrls(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(rawValue, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return List.of();
        }
    }

    private String normalizeReplySender(SupportTicketReply reply) {
        String senderType = reply.getSenderType();
        if (senderType == null || senderType.isBlank()) {
            return SUPPORT_SENDER_TYPE;
        }
        return senderType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isPartnerTicket(SupportTicket ticket) {
        return ticket.getCategoryId() != null && ticket.getCategoryId().startsWith(PARTNER_CATEGORY_PREFIX);
    }

    private String displayCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryId.startsWith(PARTNER_CATEGORY_PREFIX) ? categoryId.substring(PARTNER_CATEGORY_PREFIX.length()) : categoryId;
    }

    private void audit(String action, String entity, Long entityId, String meta) {
        adminAuditService.log(currentAdminId(), action, entity, entityId, meta);
    }

    private void publishSupportEvent(SupportTicket ticket, String eventName, Map<String, Object> payload) {
        if (supportRealtimeService == null) {
            return;
        }
        String requesterType = isPartnerTicket(ticket) ? PARTNER_REQUESTER_TYPE : CUSTOMER_REQUESTER_TYPE;
        supportRealtimeService.publish(ticket, requesterType, eventName, payload);
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }
}
