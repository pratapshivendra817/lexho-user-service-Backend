package com.lexho.user_service.admin.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.partner.entity.Partner;
import com.lexho.partner.repository.PartnerRepository;
import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.entity.*;
import com.lexho.user_service.admin.repository.*;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminManagementService {

    private final PartnerRepository partnerRepository;
    private final OrderRepository orderRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final BannerRepository bannerRepository;
    private final PayoutRepository payoutRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketReplyRepository supportTicketReplyRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;

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
        this.partnerRepository = partnerRepository;
        this.orderRepository = orderRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.bannerRepository = bannerRepository;
        this.payoutRepository = payoutRepository;
        this.activityLogRepository = activityLogRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.supportTicketReplyRepository = supportTicketReplyRepository;
        this.adminAuditService = adminAuditService;
        this.securityUtils = securityUtils;
    }

    public PagedDataResponse<Map<String, Object>> bookings(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, "id");
        OrderStatus orderStatus = parseOrderStatus(status);
        Page<Order> result = orderRepository.searchOrders(normalize(search), orderStatus, pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapOrder).collect(Collectors.toList()), result.getTotalElements());
    }

    public DataResponse<Map<String, Object>> bookingById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return new DataResponse<>(mapOrder(order));
    }

    public PagedDataResponse<Map<String, Object>> partners(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, "id");
        Page<Partner> result = partnerRepository.searchPartners(normalize(search), normalize(status), pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapProvider).collect(Collectors.toList()), result.getTotalElements());
    }

    public DataResponse<Map<String, Object>> providerById(Long id) {
        Partner partner = partnerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        return new DataResponse<>(mapProvider(partner));
    }

    @Transactional
    public DataResponse<Map<String, Object>> updateProviderVerification(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Verification status is required");
        }
        Partner partner = partnerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        partner.setVerificationStatus(status.trim().toUpperCase(Locale.ROOT));
        partnerRepository.save(partner);
        audit("PROVIDER_VERIFICATION_UPDATE", "partners", partner.getId(), "status=" + partner.getVerificationStatus());
        return new DataResponse<>(mapProvider(partner));
    }

    public PagedDataResponse<Map<String, Object>> services(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, "createdAt");
        Page<AdminServiceEntity> result = adminServiceEntityRepository.search(normalize(search), normalize(status), pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapService).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> banners(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, "createdAt");
        Boolean active = parseActiveStatus(status);
        Page<Banner> result = bannerRepository.search(normalize(search), active, pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapBanner).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> payouts(int page, int limit, String search, String status) {
        Pageable pageable = page(page, limit, "createdAt");
        Page<Payout> result = payoutRepository.search(normalize(search), normalize(status), pageable);
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapPayout).collect(Collectors.toList()), result.getTotalElements());
    }

    public PagedDataResponse<Map<String, Object>> activityLogs(int page, int limit, String search) {
        Pageable pageable = page(page, limit, "createdAt");
        Page<ActivityLog> result = activityLogRepository.search(normalize(search), pageable);
        List<Map<String, Object>> items = result.getContent().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("adminId", log.getAdminId());
            map.put("action", log.getAction());
            map.put("entity", log.getEntity());
            map.put("entityId", log.getEntityId());
            map.put("metaData", log.getMetaData());
            map.put("createdAt", log.getCreatedAt());
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
        Pageable pageable = page(page, limit, "updatedAt");
        Page<SupportTicket> result = supportTicketRepository.search(
                normalize(search),
                normalize(status),
                normalize(categoryId),
                normalize(priority),
                pageable
        );
        return new PagedDataResponse<>(result.getContent().stream().map(this::mapTicket).collect(Collectors.toList()), result.getTotalElements());
    }

    public DataResponse<Map<String, Object>> supportTicketDetail(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        return new DataResponse<>(mapTicket(ticket));
    }

    @Transactional
    public DataResponse<Map<String, Object>> updateSupportTicketStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(normalized)) {
            throw new BadRequestException("Status must be OPEN, IN_PROGRESS, RESOLVED or CLOSED");
        }
        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        ticket.setStatus(normalized);
        supportTicketRepository.save(ticket);
        audit("SUPPORT_STATUS_UPDATE", "support_tickets", ticket.getId(), "status=" + ticket.getStatus());
        return new DataResponse<>(mapTicket(ticket));
    }

    @Transactional
    public DataResponse<Map<String, Object>> replySupportTicket(Long id, String message) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Reply message is required");
        }
        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(currentAdminId());
        reply.setMessage(message.trim());
        supportTicketReplyRepository.save(reply);

        if (!"CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
            supportTicketRepository.save(ticket);
        }
        audit("SUPPORT_REPLY", "support_tickets", ticket.getId(), null);
        return new DataResponse<>(mapTicket(ticket));
    }

    private Pageable page(int page, int limit, String sortBy) {
        int safePage = Math.max(page, 1) - 1;
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, sortBy));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Boolean parseActiveStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        if ("ACTIVE".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("INACTIVE".equalsIgnoreCase(normalized)) {
            return false;
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
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("totalAmount", order.getTotalAmount());
        map.put("status", order.getStatus());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("address", order.getAddress());
        return map;
    }

    private Map<String, Object> mapProvider(Partner partner) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", partner.getId());
        map.put("name", partner.getName());
        map.put("categoryId", partner.getCategoryId());
        map.put("subCategoryId", partner.getSubCategoryId());
        map.put("rating", partner.getRating());
        map.put("price", partner.getPrice());
        map.put("available", partner.getAvailable());
        map.put("experience", partner.getExperience());
        map.put("verificationStatus", partner.getVerificationStatus());
        return map;
    }

    private Map<String, Object> mapService(AdminServiceEntity service) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", service.getId());
        map.put("name", service.getName());
        map.put("status", service.getStatus());
        map.put("categoryName", service.getCategoryName());
        map.put("createdAt", service.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapBanner(Banner banner) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", banner.getId());
        map.put("title", banner.getTitle());
        map.put("imageUrl", banner.getImageUrl());
        map.put("active", banner.isActive());
        map.put("createdAt", banner.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapPayout(Payout payout) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", payout.getId());
        map.put("providerId", payout.getProviderId());
        map.put("amount", payout.getAmount());
        map.put("status", payout.getStatus());
        map.put("createdAt", payout.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapTicket(SupportTicket ticket) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ticket.getId());
        map.put("userId", ticket.getUserId());
        map.put("subject", ticket.getSubject());
        map.put("message", ticket.getMessage());
        map.put("status", ticket.getStatus());
        map.put("categoryId", ticket.getCategoryId());
        map.put("bookingId", ticket.getBookingId());
        map.put("priority", ticket.getPriority());
        map.put("ticketId", "TCK-" + (10000 + ticket.getId()));
        map.put("createdAt", ticket.getCreatedAt());
        map.put("updatedAt", ticket.getUpdatedAt());
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "id", "MSG-1",
                "sender", "CUSTOMER",
                "message", ticket.getMessage(),
                "createdAt", ticket.getCreatedAt()
        ));
        List<SupportTicketReply> replies = supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(Map.of(
                    "id", "MSG-" + (i + 2),
                    "sender", "SUPPORT",
                    "adminId", reply.getAdminId(),
                    "message", reply.getMessage(),
                    "createdAt", reply.getCreatedAt()
            ));
        }
        map.put("messages", messages);
        return map;
    }

    private void audit(String action, String entity, Long entityId, String meta) {
        adminAuditService.log(currentAdminId(), action, entity, entityId, meta);
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }
}
