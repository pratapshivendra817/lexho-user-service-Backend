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
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDetailDto;
import com.lexho.user_service.lexhopartner.dto.PartnerSupportTicketDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerSupportServiceTest {

    private final SupportTicketRepository ticketRepository = mock(SupportTicketRepository.class);
    private final SupportTicketReplyRepository replyRepository = mock(SupportTicketReplyRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PartnerSupportService service = new PartnerSupportService(ticketRepository, replyRepository, orderRepository);

    PartnerSupportServiceTest() {
        ReflectionTestUtils.setField(service, "supportRealtimeService", new com.lexho.user_service.support.service.SupportRealtimeService());
    }

    @Test
    void createsPartnerTicketWithPartnerCategoryPrefix() {
        Order order = new Order();
        order.setId(1L);
        order.setAssignedPartnerId(7L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", 12L);
            ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
            ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 0));
            return ticket;
        });

        PartnerSupportTicketDto result = service.create(7L, new PartnerSupportCreateRequest(
                "Payment issue",
                "Payout is delayed",
                "PAYOUT",
                "1",
                "HIGH",
                List.of("https://cdn.lexho.test/partner.png")
        ));

        assertThat(result.id()).isEqualTo("TKT-000012");
        assertThat(result.categoryId()).isEqualTo("PAYOUT");
        assertThat(result.priority()).isEqualTo("HIGH");
        assertThat(result.attachmentUrls()).containsExactly("https://cdn.lexho.test/partner.png");
        verify(ticketRepository).save(any(SupportTicket.class));
    }

    @Test
    void historyQueriesOnlyPartnerTickets() {
        SupportTicket ticket = ticket(3L, "PARTNER:GENERAL");
        when(ticketRepository.findByUserIdAndCategoryIdStartingWithOrderByCreatedAtDesc(
                7L, "PARTNER:", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1));

        PageResponseDto<PartnerSupportTicketDto> result = service.history(7L, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo("TKT-000003");
        verify(ticketRepository).findByUserIdAndCategoryIdStartingWithOrderByCreatedAtDesc(
                7L, "PARTNER:", PageRequest.of(0, 20));
    }

    @Test
    void detailReturnsPartnerAndSupportMessages() {
        SupportTicket ticket = ticket(4L, "PARTNER:ORDER");
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setAdminId(1L);
        reply.setSenderType("SUPPORT");
        reply.setMessage("We are checking this.");
        reply.setAttachmentUrls("[\"https://cdn.lexho.test/reply.png\"]");
        ReflectionTestUtils.setField(reply, "id", 8L);
        ReflectionTestUtils.setField(reply, "createdAt", LocalDateTime.of(2026, 7, 20, 11, 0));
        when(ticketRepository.findByUserIdAndIdAndCategoryIdStartingWith(7L, 4L, "PARTNER:"))
                .thenReturn(Optional.of(ticket));
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(4L)).thenReturn(List.of(reply));

        PartnerSupportTicketDetailDto result = service.detail(7L, "TKT-000004");

        assertThat(result.categoryId()).isEqualTo("ORDER");
        assertThat(result.messages()).extracting("sender").containsExactly("PARTNER", "SUPPORT");
        assertThat(result.messages().get(1).attachmentUrls()).containsExactly("https://cdn.lexho.test/reply.png");
    }

    @Test
    void detailRejectsTicketsOutsidePartnerScope() {
        when(ticketRepository.findByUserIdAndIdAndCategoryIdStartingWith(7L, 4L, "PARTNER:"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(7L, "TKT-000004"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    void replyAndStatusUpdateShouldSupportTwoWayFlow() {
        SupportTicket ticket = ticket(5L, "PARTNER:ORDER");
        ticket.setStatus("RESOLVED");
        when(ticketRepository.findByUserIdAndIdAndCategoryIdStartingWith(7L, 5L, "PARTNER:"))
                .thenReturn(Optional.of(ticket));
        when(replyRepository.save(any(SupportTicketReply.class))).thenAnswer(invocation -> {
            SupportTicketReply saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 7, 20, 12, 0));
            return saved;
        });
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(5L)).thenReturn(List.of(new SupportTicketReply()));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerSupportMessageDto message = service.reply(7L, "TKT-000005",
                new com.lexho.user_service.lexhopartner.dto.PartnerSupportReplyRequest(
                        "sharing screenshot",
                        List.of("https://cdn.lexho.test/partner-reply.png")));

        assertThat(message.sender()).isEqualTo("PARTNER");
        assertThat(message.attachmentUrls()).containsExactly("https://cdn.lexho.test/partner-reply.png");
        assertThat(ticket.getStatus()).isEqualTo("OPEN");

        PartnerSupportTicketDto closed = service.updateStatus(7L, "TKT-000005", "CLOSED");
        assertThat(closed.status()).isEqualTo("CLOSED");

        PartnerSupportTicketDto reopened = service.updateStatus(7L, "TKT-000005", "OPEN");
        assertThat(reopened.status()).isEqualTo("OPEN");
    }

    @Test
    void createShouldRejectForeignBooking() {
        Order order = new Order();
        order.setId(2L);
        order.setAssignedPartnerId(8L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.create(7L, new PartnerSupportCreateRequest(
                "Payment issue",
                "Payout is delayed",
                "PAYOUT",
                "2",
                "HIGH",
                List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed to link this booking");
    }

    @Test
    void streamShouldOpenForOwnedPartnerTicket() {
        SupportTicket ticket = ticket(6L, "PARTNER:GENERAL");
        when(ticketRepository.findByUserIdAndIdAndCategoryIdStartingWith(7L, 6L, "PARTNER:"))
                .thenReturn(Optional.of(ticket));

        SseEmitter emitter = service.stream(7L, "TKT-000006");

        assertThat(emitter).isNotNull();
    }

    private SupportTicket ticket(Long id, String categoryId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(7L);
        ticket.setSubject("Need help");
        ticket.setMessage("Please help");
        ticket.setStatus("OPEN");
        ticket.setCategoryId(categoryId);
        ticket.setBookingId("BK-1");
        ticket.setPriority("MEDIUM");
        ticket.setAttachmentUrls("[\"https://cdn.lexho.test/ticket.png\"]");
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.of(2026, 7, 20, 10, 0));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.of(2026, 7, 20, 10, 5));
        return ticket;
    }
}
