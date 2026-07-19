package com.lexho.user_service.admin;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.admin.service.AdminBookingManagementService;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.map.service.NavigationEtaPushService;
import com.lexho.user_service.map.service.PartnerNavigationService;
import com.lexho.user_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBookingManagementServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final BookingEventRepository bookingEventRepository = mock(BookingEventRepository.class);
    private final CurrentPartnerLocationRepository locationRepository = mock(CurrentPartnerLocationRepository.class);
    private final PartnerOrderRepository partnerOrderRepository = mock(PartnerOrderRepository.class);
    private final PayoutRepository payoutRepository = mock(PayoutRepository.class);
    private final PartnerNavigationService partnerNavigationService = mock(PartnerNavigationService.class);
    private final NavigationEtaPushService navigationEtaPushService = mock(NavigationEtaPushService.class);
    private final AdminBookingManagementService service = new AdminBookingManagementService(
            orderRepository, orderService, bookingEventRepository, locationRepository, partnerOrderRepository,
            payoutRepository, partnerNavigationService, navigationEtaPushService);

    @Test
    void listShouldNormalizeInputsAndMapRows() {
        Order order = order(1L, OrderStatus.CREATED, PaymentStatus.PENDING);
        when(orderRepository.searchOrders(org.mockito.ArgumentMatchers.eq("abc"),
                org.mockito.ArgumentMatchers.eq(OrderStatus.CREATED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Map<String, Object> response = service.list(" abc ", "created", -1, 500, "bad", "asc");

        assertEquals(0, response.get("page"));
        assertEquals(1L, response.get("totalElements"));
        List<?> items = (List<?>) response.get("items");
        assertEquals(1, items.size());
    }

    @Test
    void updateStatusShouldValidateAndDelegateToOrderService() {
        Order order = order(1L, OrderStatus.CREATED, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(1L);
        dto.setStatus("CONFIRMED");
        dto.setPaymentStatus("PENDING");
        dto.setTotalAmount(100.0);
        when(orderService.updateStatus(1L, OrderStatus.CONFIRMED)).thenReturn(dto);

        Map<String, Object> response = service.updateStatus(1L, OrderStatus.CONFIRMED);

        assertEquals("CONFIRMED", response.get("status"));
        assertThrows(BadRequestException.class, () -> service.updateStatus(1L, null));
        assertThrows(BadRequestException.class, () -> service.updateStatus(1L, OrderStatus.DELIVERED));
    }

    @Test
    void cancelShouldSoftCancelOnlyCancellableOrders() {
        Order order = order(1L, OrderStatus.CREATED, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertEquals("CANCELLED", service.cancel(1L).get("status"));

        Order delivered = order(2L, OrderStatus.DELIVERED, PaymentStatus.SUCCESS);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(delivered));
        assertThrows(BadRequestException.class, () -> service.cancel(2L));
    }

    @Test
    void overridePartnerLocationShouldCreateOrUpdateLocation() {
        PartnerOrder partnerOrder = new PartnerOrder();
        partnerOrder.setAssignedPartnerId(77L);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(partnerOrder));
        when(locationRepository.findByPartnerIdAndDeletedFalse(77L)).thenReturn(Optional.empty());
        when(locationRepository.save(any(CurrentPartnerLocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partnerNavigationService.etaForBooking(1L)).thenReturn(navigation(1L));

        Map<String, Object> response = service.overridePartnerLocation(1L, 28.6, 77.2, "110001");

        assertEquals(77L, response.get("partnerId"));
        assertNotNull(response.get("navigation"));
        verify(navigationEtaPushService).publish(1L, navigation(1L));
        assertThrows(BadRequestException.class, () -> service.overridePartnerLocation(1L, null, 77.2, "110001"));
        PartnerOrder unassigned = new PartnerOrder();
        when(partnerOrderRepository.findById(2L)).thenReturn(Optional.of(unassigned));
        assertThrows(BadRequestException.class, () -> service.overridePartnerLocation(2L, 28.6, 77.2, "110001"));
    }

    @Test
    void liveTrackingAndNavigationShouldIncludeNavigationEta() {
        Order order = order(11L, OrderStatus.CONFIRMED, PaymentStatus.PENDING);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        PartnerOrder partnerOrder = new PartnerOrder();
        partnerOrder.setAssignedPartnerId(55L);
        when(partnerOrderRepository.findById(11L)).thenReturn(Optional.of(partnerOrder));
        when(locationRepository.findByPartnerIdAndDeletedFalse(55L)).thenReturn(Optional.empty());
        when(bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(11L)).thenReturn(List.of());
        when(partnerNavigationService.etaForBooking(11L)).thenReturn(navigation(11L));

        Map<String, Object> liveTracking = service.liveTracking(11L);
        NavigationRouteResponse response = service.navigation(11L);

        assertNotNull(liveTracking.get("navigation"));
        assertEquals(11L, response.bookingId());
        verify(partnerNavigationService, org.mockito.Mockito.times(2)).etaForBooking(11L);
    }

    @Test
    void updatePaymentStatusShouldRejectUnsafeStatusesAndCreateAuditEvent() {
        Order order = order(1L, OrderStatus.CREATED, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Map<String, Object> response = service.updatePaymentStatus(1L, PaymentStatus.FAILED,
                "customer said \"failed\"", 99L);

        assertEquals("FAILED", response.get("paymentStatus"));
        verify(bookingEventRepository).save(any());
        assertThrows(BadRequestException.class, () -> service.updatePaymentStatus(1L, null, "reason", 99L));
        assertThrows(BadRequestException.class, () -> service.updatePaymentStatus(1L, PaymentStatus.SUCCESS, "reason", 99L));
        assertThrows(BadRequestException.class, () -> service.updatePaymentStatus(1L, PaymentStatus.FAILED, " ", 99L));
    }

    @Test
    void payoutAndReconciliationShouldMapPaymentData() {
        Order order = order(1L, OrderStatus.DELIVERED, PaymentStatus.SUCCESS);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        Payout payout = new Payout();
        payout.setProviderId(88L);
        payout.setReferenceBookingId(1L);
        payout.setAmount(500.0);
        payout.setStatus("PENDING");
        when(payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> {
            Payout saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 501L);
            return saved;
        });

        assertEquals("SUCCESS", service.paymentReconciliation(1L).get("paymentStatus"));
        assertEquals("APPROVED", service.createOrUpdatePayout(1L, 88L, 500.0, " approved ").get("status"));
        assertThrows(BadRequestException.class, () -> service.createOrUpdatePayout(1L, null, 500.0, "PENDING"));
        assertThrows(ResourceNotFoundException.class, () -> service.byId(404L));
    }

    private Order order(Long id, OrderStatus status, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(10L);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setTotalAmount(100.0);
        order.setAddress("Delhi");
        order.setBookingType("INSTANT");
        order.onCreate();
        return order;
    }

    private NavigationRouteResponse navigation(Long bookingId) {
        return new NavigationRouteResponse(
                bookingId,
                4.2,
                12,
                "polyline",
                java.time.LocalDateTime.of(2026, 6, 14, 20, 45),
                true,
                "MAPBOX",
                false,
                true,
                false,
                null,
                2,
                true,
                28.61,
                77.20,
                java.time.LocalDateTime.of(2026, 6, 14, 20, 33),
                28.62,
                77.21,
                java.time.LocalDateTime.of(2026, 6, 14, 20, 34)
        );
    }
}
