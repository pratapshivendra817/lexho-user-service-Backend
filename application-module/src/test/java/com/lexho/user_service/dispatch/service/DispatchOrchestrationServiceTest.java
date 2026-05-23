package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.repository.PincodeAdjacencyRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.partner.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchOrchestrationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private LexhoAddressRepository lexhoAddressRepository;
    @Mock
    private ServiceableAreaRepository serviceableAreaRepository;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @Mock
    private PincodeAdjacencyRepository pincodeAdjacencyRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerServiceRequestRepository partnerServiceRequestRepository;
    @Mock
    private BookingEventRepository bookingEventRepository;
    @Mock
    private DispatchScoringPolicy dispatchScoringPolicy;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DispatchOrchestrationService dispatchOrchestrationService;

    @Test
    void markPartnerAcceptedInstantShouldSetPromisedStartTimeAndAssignedState() {
        Order order = new Order();
        order.setId(1L);
        order.setBookingType("INSTANT");
        order.setStatus(OrderStatus.SEARCHING_PARTNER);

        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setActiveJobsCount(1);

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(partnerShiftRepository.findByPartnerId(99L)).thenReturn(Optional.of(shift));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerShiftRepository.save(any(PartnerShiftEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        dispatchOrchestrationService.markPartnerAccepted(1L, 99L, 12);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(OrderStatus.PARTNER_ASSIGNED, saved.getStatus());
        assertEquals(DispatchOrchestrationService.INSTANT_SLA_MINUTES, saved.getSlaMinutes());
        assertNotNull(saved.getPromisedStartTime());
        assertEquals("INSTANT", saved.getBookingType());
        LocalDateTime minExpected = before.plusMinutes(12).minusSeconds(2);
        LocalDateTime maxExpected = after.plusMinutes(12).plusSeconds(2);
        boolean inRange = !saved.getPromisedStartTime().isBefore(minExpected)
                && !saved.getPromisedStartTime().isAfter(maxExpected);
        assertTrue(inRange);
    }
}
