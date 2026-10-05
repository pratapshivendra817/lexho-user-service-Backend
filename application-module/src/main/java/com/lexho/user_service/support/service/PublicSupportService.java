package com.lexho.user_service.support.service;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.support.dto.PublicSupportTicketRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PublicSupportService {

    private static final Long PUBLIC_REQUESTER_USER_ID = 0L;
    private static final List<String> PUBLIC_FAQ_USER_TYPES = List.of("GENERAL", "CUSTOMER");

    private final SupportFaqCategoryRepository categoryRepository;
    private final SupportFaqRepository faqRepository;
    private final SupportTicketRepository ticketRepository;

    public PublicSupportService(SupportFaqCategoryRepository categoryRepository,
                                SupportFaqRepository faqRepository,
                                SupportTicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.faqRepository = faqRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicFaqs() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(category -> Map.<String, Object>of(
                        "id", category.getCategoryKey(),
                        "label", category.getLabel(),
                        "faqs", faqRepository
                                .findByCategoryAndUserTypeInAndActiveTrueOrderByDisplayOrderAscIdAsc(
                                        category,
                                        PUBLIC_FAQ_USER_TYPES
                                )
                                .stream()
                                .map(this::mapFaq)
                                .toList()
                ))
                .filter(category -> !((List<?>) category.get("faqs")).isEmpty())
                .toList();
    }

    @Transactional
    public Map<String, Object> createTicket(PublicSupportTicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(PUBLIC_REQUESTER_USER_ID);
        ticket.setRequesterName(trim(request.name()));
        ticket.setRequesterEmail(trim(request.email()).toLowerCase());
        ticket.setRequesterPhone(blankToNull(request.phone()));
        ticket.setSubject(trim(request.subject()));
        ticket.setMessage(trim(request.message()));
        ticket.setBookingId(blankToNull(request.bookingId()));
        ticket.setCategoryId("PUBLIC");
        ticket.setPriority("MEDIUM");
        ticket.setStatus("OPEN");

        SupportTicket saved = ticketRepository.save(ticket);
        return Map.of(
                "id", formatTicketId(saved.getId()),
                "ticketId", formatTicketId(saved.getId()),
                "status", saved.getStatus(),
                "subject", saved.getSubject(),
                "bookingId", saved.getBookingId() == null ? "" : saved.getBookingId(),
                "createdAt", String.valueOf(saved.getCreatedAt())
        );
    }

    private Map<String, Object> mapFaq(SupportFaq faq) {
        return Map.of(
                "id", faq.getFaqKey(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer()
        );
    }

    private String formatTicketId(Long id) {
        return "TCK-" + (10000 + id);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
