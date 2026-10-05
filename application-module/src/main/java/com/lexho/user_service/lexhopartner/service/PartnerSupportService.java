package com.lexho.user_service.lexhopartner.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import com.lexho.user_service.admin.repository.SupportTicketReplyRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportCreateRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportMessageDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportReplyRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDetailDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDto;
import com.lexho.user_service.support.service.SupportRealtimeService;
import com.lexho.user_service.util.LexhoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PartnerSupportService {

    private static final String CATEGORY_PREFIX = "PARTNER:";
    private static final String KEY_STATUS = "status";

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketReplyRepository supportTicketReplyRepository;
    private final OrderRepository orderRepository;
    private final SupportRealtimeService supportRealtimeService;

    public PartnerSupportService(SupportTicketRepository supportTicketRepository,
                                 SupportTicketReplyRepository supportTicketReplyRepository,
                                 OrderRepository orderRepository) {
        this(supportTicketRepository, supportTicketReplyRepository, orderRepository, Optional.empty());
    }

    @Autowired
    public PartnerSupportService(SupportTicketRepository supportTicketRepository,
                                 SupportTicketReplyRepository supportTicketReplyRepository,
                                 OrderRepository orderRepository,
                                 Optional<SupportRealtimeService> supportRealtimeService) {
        this.supportTicketRepository = supportTicketRepository;
        this.supportTicketReplyRepository = supportTicketReplyRepository;
        this.orderRepository = orderRepository;
        this.supportRealtimeService = supportRealtimeService.orElse(null);
    }

    public PartnerSupportTicketDto create(Long partnerId, PartnerSupportCreateRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(partnerId);
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setStatus(PartnerServiceText.STATUS_OPEN);
        ticket.setCategoryId(partnerCategory(request.categoryId()));
        ticket.setBookingId(normalizePartnerBookingId(partnerId, request.bookingId()));
        ticket.setPriority(normalizePriority(request.priority()));
        ticket.setAttachmentUrls(PartnerAttachmentUrlSupport.serializeAttachmentUrls(request.attachmentUrls()));
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, PartnerServiceText.EVENT_TICKET_CREATED, Map.of(
                "id", formatTicketId(saved.getId()),
                KEY_STATUS, saved.getStatus(),
                    LexhoConstants.FIELD_UPDATED_AT, String.valueOf(saved.getUpdatedAt())
        ));
        return mapTicket(saved);
    }

    public PageResponseDto<PartnerSupportTicketDto> history(Long partnerId, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<SupportTicket> tickets = status == null || status.isBlank()
                ? supportTicketRepository.findByUserIdAndCategoryIdStartingWithOrderByCreatedAtDesc(
                partnerId, CATEGORY_PREFIX, pageable)
                : supportTicketRepository.findByUserIdAndStatusIgnoreCaseAndCategoryIdStartingWithOrderByCreatedAtDesc(
                partnerId, status.trim(), CATEGORY_PREFIX, pageable);
        List<PartnerSupportTicketDto> content = tickets.getContent().stream()
                .map(this::mapTicket)
                .toList();
        return new PageResponseDto<>(content, safePage, safeSize, tickets.getTotalElements(), tickets.getTotalPages());
    }

    public PartnerSupportTicketDetailDto detail(Long partnerId, String ticketId) {
        SupportTicket ticket = requirePartnerTicket(partnerId, ticketId);
        List<SupportTicketReply> replies = supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());

        List<PartnerSupportMessageDto> messages = new ArrayList<>();
        messages.add(new PartnerSupportMessageDto(
                "MSG-1",
                PartnerServiceText.SENDER_PARTNER,
                ticket.getMessage(),
                PartnerAttachmentUrlSupport.deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                String.valueOf(ticket.getCreatedAt())
        ));
        for (int i = 0; i < replies.size(); i++) {
            SupportTicketReply reply = replies.get(i);
            messages.add(new PartnerSupportMessageDto(
                    "MSG-" + (i + 2),
                    normalizeSender(reply),
                    reply.getMessage(),
                    PartnerAttachmentUrlSupport.deserializeAttachmentUrls(reply.getAttachmentUrls()),
                    String.valueOf(reply.getCreatedAt())
            ));
        }

        return new PartnerSupportTicketDetailDto(
                formatTicketId(ticket.getId()),
                ticket.getSubject(),
                ticket.getStatus(),
                stripPartnerCategory(ticket.getCategoryId()),
                ticket.getBookingId(),
                ticket.getPriority(),
                PartnerAttachmentUrlSupport.deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                String.valueOf(ticket.getCreatedAt()),
                String.valueOf(ticket.getUpdatedAt()),
                messages
        );
    }

    public PartnerSupportMessageDto reply(Long partnerId, String ticketId, PartnerSupportReplyRequest request) {
        SupportTicket ticket = requirePartnerTicket(partnerId, ticketId);
        if (PartnerServiceText.STATUS_CLOSED.equalsIgnoreCase(ticket.getStatus())) {
            throw new BadRequestException(PartnerServiceText.MSG_CLOSED_TICKETS_CANNOT_BE_REPLIED);
        }
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(null);
        reply.setSenderType(PartnerServiceText.SENDER_PARTNER);
        reply.setMessage(request.message().trim());
        reply.setAttachmentUrls(PartnerAttachmentUrlSupport.serializeAttachmentUrls(request.attachmentUrls()));
        SupportTicketReply saved = supportTicketReplyRepository.save(reply);
        if (PartnerServiceText.STATUS_RESOLVED.equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus(PartnerServiceText.STATUS_OPEN);
            supportTicketRepository.save(ticket);
        }
        publishSupportEvent(ticket, PartnerServiceText.EVENT_TICKET_MESSAGE, Map.of(
                "ticketId", formatTicketId(ticket.getId()),
                KEY_STATUS, ticket.getStatus(),
                "message", Map.of(
                        "sender", PartnerServiceText.SENDER_PARTNER,
                        "text", saved.getMessage(),
                                "attachmentUrls", PartnerAttachmentUrlSupport.deserializeAttachmentUrls(saved.getAttachmentUrls()),
                        "createdAt", String.valueOf(saved.getCreatedAt())
                )
        ));
        return new PartnerSupportMessageDto(
                "MSG-" + (supportTicketReplyRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId()).size() + 1),
                PartnerServiceText.SENDER_PARTNER,
                saved.getMessage(),
                PartnerAttachmentUrlSupport.deserializeAttachmentUrls(saved.getAttachmentUrls()),
                String.valueOf(saved.getCreatedAt())
        );
    }

    public PartnerSupportTicketDto updateStatus(Long partnerId, String ticketId, String status) {
        SupportTicket ticket = requirePartnerTicket(partnerId, ticketId);
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (PartnerServiceText.STATUS_CLOSED.equals(normalized)) {
            if (PartnerServiceText.STATUS_CLOSED.equalsIgnoreCase(ticket.getStatus())) {
                throw new BadRequestException(PartnerServiceText.MSG_TICKET_ALREADY_CLOSED);
            }
            ticket.setStatus(PartnerServiceText.STATUS_CLOSED);
        } else if (PartnerServiceText.STATUS_OPEN.equals(normalized)) {
            if (!List.of(PartnerServiceText.STATUS_CLOSED, PartnerServiceText.STATUS_RESOLVED)
                    .contains(ticket.getStatus() == null ? "" : ticket.getStatus().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException(PartnerServiceText.MSG_ONLY_CLOSED_OR_RESOLVED_REOPEN);
            }
            ticket.setStatus(PartnerServiceText.STATUS_OPEN);
        } else {
            throw new BadRequestException(PartnerServiceText.MSG_STATUS_OPEN_OR_CLOSED);
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        publishSupportEvent(saved, PartnerServiceText.EVENT_TICKET_STATUS, Map.of(
                "id", formatTicketId(saved.getId()),
                KEY_STATUS, saved.getStatus(),
                    LexhoConstants.FIELD_UPDATED_AT, String.valueOf(saved.getUpdatedAt())
        ));
        return mapTicket(saved);
    }

    public SseEmitter stream(Long partnerId, String ticketId) {
        SupportTicket ticket = requirePartnerTicket(partnerId, ticketId);
        if (supportRealtimeService == null) {
            throw new BadRequestException(PartnerServiceText.MSG_STREAM_NOT_AVAILABLE);
        }
        return supportRealtimeService.subscribe(PartnerServiceText.SENDER_PARTNER, partnerId, ticket.getId());
    }

    private PartnerSupportTicketDto mapTicket(SupportTicket ticket) {
        return new PartnerSupportTicketDto(
                formatTicketId(ticket.getId()),
                ticket.getSubject(),
                ticket.getMessage(),
                ticket.getStatus(),
                stripPartnerCategory(ticket.getCategoryId()),
                ticket.getBookingId(),
                ticket.getPriority() == null ? PartnerServiceText.STATUS_MEDIUM : ticket.getPriority(),
                PartnerAttachmentUrlSupport.deserializeAttachmentUrls(ticket.getAttachmentUrls()),
                String.valueOf(ticket.getCreatedAt()),
                String.valueOf(ticket.getUpdatedAt())
        );
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return PartnerServiceText.STATUS_MEDIUM;
        }
        return priority.trim().toUpperCase(Locale.ROOT);
    }

    private String partnerCategory(String categoryId) {
        String normalized = blankToNull(categoryId);
        return CATEGORY_PREFIX + (normalized == null ? "GENERAL" : normalized);
    }

    private String stripPartnerCategory(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryId.startsWith(CATEGORY_PREFIX)
                ? categoryId.substring(CATEGORY_PREFIX.length())
                : categoryId;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupportTicket requirePartnerTicket(Long partnerId, String ticketId) {
        Long id = parseTicketId(ticketId);
        return supportTicketRepository
                .findByUserIdAndIdAndCategoryIdStartingWith(partnerId, id, CATEGORY_PREFIX)
                .orElseThrow(() -> new BadRequestException(PartnerServiceText.MSG_TICKET_NOT_FOUND));
    }

    private String normalizePartnerBookingId(Long partnerId, String bookingId) {
        String normalized = blankToNull(bookingId);
        if (normalized == null) {
            return null;
        }
        String digits = normalized.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new BadRequestException(PartnerServiceText.MSG_INVALID_BOOKING_ID);
        }
        Long resolvedBookingId = Long.parseLong(digits);
        Order order = orderRepository.findById(resolvedBookingId)
                .orElseThrow(() -> new BadRequestException(PartnerServiceText.MSG_BOOKING_NOT_FOUND));
        if (!partnerId.equals(order.getAssignedPartnerId())) {
            throw new BadRequestException(PartnerServiceText.MSG_LINK_BOOKING_FORBIDDEN);
        }
        return String.valueOf(resolvedBookingId);
    }

    private String normalizeSender(SupportTicketReply reply) {
        String senderType = reply.getSenderType();
        if (senderType == null || senderType.isBlank()) {
            return PartnerServiceText.SENDER_SUPPORT;
        }
        return senderType.trim().toUpperCase(Locale.ROOT);
    }

    private String formatTicketId(Long id) {
        return "TKT-" + String.format("%06d", id);
    }

    private Long parseTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new BadRequestException(PartnerServiceText.MSG_TICKET_ID_REQUIRED);
        }
        String digits = ticketId.trim().replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new BadRequestException(PartnerServiceText.MSG_INVALID_TICKET_ID);
        }
        return Long.parseLong(digits);
    }

    private void publishSupportEvent(SupportTicket ticket, String eventName, Map<String, Object> payload) {
        if (supportRealtimeService != null) {
            supportRealtimeService.publish(ticket, PartnerServiceText.SENDER_PARTNER, eventName, payload);
        }
    }
}
