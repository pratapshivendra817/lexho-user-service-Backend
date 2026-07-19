package com.lexho.user_service.admin;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.entity.ActivityLog;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.Banner;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.entity.SupportTicket;
import com.lexho.user_service.admin.entity.SupportTicketReply;
import com.lexho.user_service.admin.repository.*;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.admin.service.AdminManagementService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {
    @Mock PartnerRepository partnerRepository;
    @Mock OrderRepository orderRepository;
    @Mock AdminServiceEntityRepository serviceRepository;
    @Mock BannerRepository bannerRepository;
    @Mock PayoutRepository payoutRepository;
    @Mock ActivityLogRepository activityLogRepository;
    @Mock SupportTicketRepository ticketRepository;
    @Mock SupportTicketReplyRepository replyRepository;
    @Mock AdminAuditService auditService;
    @Mock SecurityUtils securityUtils;
    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminManagementService(partnerRepository, orderRepository, serviceRepository,
                bannerRepository, payoutRepository, activityLogRepository, ticketRepository,
                replyRepository, auditService, securityUtils);
    }

    @Test
    void bookingsNormalizesPagingAndMapsOrders() {
        Order order = new Order();
        order.setId(1L); order.setUserId(2L); order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING); order.setTotalAmount(120.0); order.setAddress("A");
        when(orderRepository.searchOrders(isNull(), eq(OrderStatus.CREATED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        var result = service.bookings(0, 500, "  ", "created");

        assertEquals(1, result.getTotal());
        assertEquals(1L, result.getData().get(0).get("id"));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).searchOrders(isNull(), eq(OrderStatus.CREATED), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void bookingsRejectsInvalidStatusAndMissingBooking() {
        assertThrows(BadRequestException.class, () -> service.bookings(1, 10, null, "wrong"));
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.bookingById(9L));
    }

    @Test
    void partnersAndProviderDetailMapRepositoryResults() {
        Partner partner = partner(3L);
        when(partnerRepository.searchPartners(eq("clean"), eq("active"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(partner)));
        assertEquals("Partner", service.partners(1, 10, " clean ", " active ").getData().get(0).get("name"));

        when(partnerRepository.findById(3L)).thenReturn(Optional.of(partner));
        assertEquals(3L, service.providerById(3L).getData().get("id"));
    }

    @Test
    void updateVerificationValidatesPersistsAndAudits() {
        assertThrows(BadRequestException.class, () -> service.updateProviderVerification(3L, " "));
        Partner partner = partner(3L);
        when(partnerRepository.findById(3L)).thenReturn(Optional.of(partner));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(77L));

        var result = service.updateProviderVerification(3L, " verified ");

        assertEquals("VERIFIED", result.getData().get("verificationStatus"));
        verify(partnerRepository).save(partner);
        verify(auditService).log(77L, "PROVIDER_VERIFICATION_UPDATE", "partners", 3L, "status=VERIFIED");
    }

    @Test
    void bannersRejectInvalidFilter() {
        assertThrows(BadRequestException.class, () -> service.banners(1, 10, null, "unknown"));
    }

    @Test
    void servicesBannersPayoutsAndActivityLogsMapResults() {
        AdminServiceEntity adminService = new AdminServiceEntity();
        adminService.setName("Deep Cleaning");
        adminService.setStatus("ACTIVE");
        adminService.setCategoryName("Home");
        ReflectionTestUtils.setField(adminService, "id", 4L);
        ReflectionTestUtils.setField(adminService, "createdAt", LocalDateTime.now());

        Banner banner = new Banner();
        ReflectionTestUtils.setField(banner, "id", 5L);
        banner.setTitle("Offer");
        banner.setImageUrl("/offer.png");
        banner.setActive(true);
        ReflectionTestUtils.setField(banner, "createdAt", LocalDateTime.now());

        Payout payout = new Payout();
        ReflectionTestUtils.setField(payout, "id", 6L);
        payout.setProviderId(10L);
        payout.setAmount(250.0);
        payout.setStatus("PAID");
        ReflectionTestUtils.setField(payout, "createdAt", LocalDateTime.now());

        ActivityLog log = new ActivityLog();
        ReflectionTestUtils.setField(log, "id", 7L);
        log.setAdminId(9L);
        log.setAction("CREATE");
        log.setEntity("banner");
        log.setEntityId(5L);
        log.setMetaData("meta");
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.now());

        when(serviceRepository.search(eq("clean"), isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(adminService)));
        when(bannerRepository.search(eq("offer"), eq(Boolean.TRUE), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(banner)));
        when(payoutRepository.search(eq("paid"), eq("PAID"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(payout)));
        when(activityLogRepository.search(eq("meta"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        assertEquals("Deep Cleaning", service.services(1, 10, " clean ", null).getData().get(0).get("name"));
        assertEquals("Offer", service.banners(1, 10, " offer ", "ACTIVE").getData().get(0).get("title"));
        assertEquals("PAID", service.payouts(1, 10, " paid ", " PAID ").getData().get(0).get("status"));
        assertEquals("CREATE", service.activityLogs(1, 10, " meta ").getData().get(0).get("action"));
    }

    @Test
    void supportTicketsSearchAndMissingDetailBranches() {
        SupportTicket ticket = ticket(5L, "OPEN");
        when(ticketRepository.search(eq("find"), eq("OPEN"), eq("GENERAL"), eq("HIGH"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        assertEquals(1, service.supportTickets(1, 10, " find ", " OPEN ", " GENERAL ", " HIGH ").getTotal());

        when(ticketRepository.findById(55L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.supportTicketDetail(55L));
        assertThrows(ResourceNotFoundException.class, () -> service.updateSupportTicketStatus(55L, "OPEN"));
        assertThrows(ResourceNotFoundException.class, () -> service.replySupportTicket(55L, "reply"));
    }

    @Test
    void supportTicketDetailIncludesCustomerAndSupportMessages() {
        SupportTicket ticket = ticket(5L, "OPEN");
        SupportTicketReply reply = new SupportTicketReply();
        reply.setAdminId(8L); reply.setMessage("Reply"); reply.onCreate();
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(5L)).thenReturn(List.of(reply));

        var data = service.supportTicketDetail(5L).getData();

        assertEquals("TCK-10005", data.get("ticketId"));
        assertEquals(2, ((List<?>) data.get("messages")).size());
    }

    @Test
    void updateSupportStatusValidatesAllowedValues() {
        assertThrows(BadRequestException.class, () -> service.updateSupportTicketStatus(5L, null));
        assertThrows(BadRequestException.class, () -> service.updateSupportTicketStatus(5L, "WAITING"));
        SupportTicket ticket = ticket(5L, "OPEN");
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(5L)).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        assertEquals("RESOLVED", service.updateSupportTicketStatus(5L, " resolved ").getData().get("status"));
        verify(ticketRepository).save(ticket);
        verify(auditService).log(0L, "SUPPORT_STATUS_UPDATE", "support_tickets", 5L, "status=RESOLVED");
    }

    @Test
    void replySupportTicketCreatesReplyAndMovesOpenTicket() {
        assertThrows(BadRequestException.class, () -> service.replySupportTicket(5L, " "));
        SupportTicket ticket = ticket(5L, "OPEN");
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(5L)).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(42L));

        service.replySupportTicket(5L, "  We are checking  ");

        ArgumentCaptor<SupportTicketReply> captor = ArgumentCaptor.forClass(SupportTicketReply.class);
        verify(replyRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getAdminId());
        assertEquals("We are checking", captor.getValue().getMessage());
        assertEquals("IN_PROGRESS", ticket.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void replySupportTicketLeavesClosedTicketClosedAndUsesDefaultAdminId() {
        SupportTicket ticket = ticket(6L, "CLOSED");
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(ticket));
        when(replyRepository.findByTicket_IdOrderByCreatedAtAsc(6L)).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        Map<String, Object> data = service.replySupportTicket(6L, "done").getData();

        assertEquals("CLOSED", ticket.getStatus());
        verify(ticketRepository, never()).save(ticket);
        verify(auditService).log(0L, "SUPPORT_REPLY", "support_tickets", 6L, null);
        assertEquals("CLOSED", data.get("status"));
    }

    private Partner partner(Long id) {
        Partner partner = new Partner();
        partner.setId(id); partner.setName("Partner"); partner.setCategoryId(1L);
        partner.setRating(4.5); partner.setPrice(100.0); partner.setAvailable(true);
        return partner;
    }

    private SupportTicket ticket(Long id, String status) {
        SupportTicket ticket = new SupportTicket();
        ReflectionTestUtils.setField(ticket, "id", id);
        ticket.setUserId(10L); ticket.setSubject("Subject"); ticket.setMessage("Message");
        ticket.setStatus(status); ticket.setCategoryId("GENERAL"); ticket.setPriority("HIGH");
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.now());
        return ticket;
    }
}
