package com.lexho.user_service.support;

import com.lexho.user_service.admin.entity.SupportFaq;
import com.lexho.user_service.admin.entity.SupportFaqCategory;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.repository.SupportFaqCategoryRepository;
import com.lexho.user_service.admin.repository.SupportFaqRepository;
import com.lexho.user_service.admin.repository.SupportTicketRepository;
import com.lexho.user_service.support.dto.PublicSupportTicketRequest;
import com.lexho.user_service.support.service.PublicSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSupportServiceTest {

    @Mock
    private SupportFaqCategoryRepository categoryRepository;
    @Mock
    private SupportFaqRepository faqRepository;
    @Mock
    private SupportTicketRepository ticketRepository;

    private PublicSupportService service;

    @BeforeEach
    void setUp() {
        service = new PublicSupportService(categoryRepository, faqRepository, ticketRepository);
    }

    @Test
    void publicFaqsReturnActiveGeneralAndCustomerFaqsGroupedByCategory() {
        SupportFaqCategory bookings = category("bookings", "Bookings");
        SupportFaq faq = faq("booking-cancel", bookings);
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(bookings));
        when(faqRepository.findByCategoryAndUserTypeInAndActiveTrueOrderByDisplayOrderAscIdAsc(
                bookings,
                List.of("GENERAL", "CUSTOMER")
        )).thenReturn(List.of(faq));

        List<Map<String, Object>> result = service.publicFaqs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("label", "Bookings");
        assertThat((List<?>) result.get(0).get("faqs")).hasSize(1);
    }

    @Test
    void createTicketStoresPublicRequesterDetails() throws Exception {
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            setField(ticket, "id", 7L);
            setField(ticket, "createdAt", LocalDateTime.of(2026, 8, 23, 10, 0));
            return ticket;
        });

        Map<String, Object> result = service.createTicket(new PublicSupportTicketRequest(
                " Customer name ",
                "CUSTOMER@EXAMPLE.COM ",
                " 9876543210 ",
                " Booking issue ",
                " Booking ID #123 related problem ",
                " 123 "
        ));

        ArgumentCaptor<SupportTicket> captor = ArgumentCaptor.forClass(SupportTicket.class);
        verify(ticketRepository).save(captor.capture());
        SupportTicket saved = captor.getValue();
        assertThat(saved.getUserId()).isZero();
        assertThat(saved.getRequesterName()).isEqualTo("Customer name");
        assertThat(saved.getRequesterEmail()).isEqualTo("customer@example.com");
        assertThat(saved.getRequesterPhone()).isEqualTo("9876543210");
        assertThat(saved.getCategoryId()).isEqualTo("PUBLIC");
        assertThat(saved.getPriority()).isEqualTo("MEDIUM");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(result).containsEntry("id", "TCK-10007");
    }

    private SupportFaqCategory category(String key, String label) {
        SupportFaqCategory category = new SupportFaqCategory();
        category.setCategoryKey(key);
        category.setLabel(label);
        category.setDisplayOrder(1);
        category.setActive(true);
        return category;
    }

    private SupportFaq faq(String key, SupportFaqCategory category) {
        SupportFaq faq = new SupportFaq();
        faq.setFaqKey(key);
        faq.setCategory(category);
        faq.setQuestion("Booking cancel kaise karein?");
        faq.setAnswer("Use booking detail screen.");
        faq.setDisplayOrder(1);
        faq.setActive(true);
        faq.setUserType("GENERAL");
        return faq;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
