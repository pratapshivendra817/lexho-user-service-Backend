package com.lexho.user_service.partnerrequest.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.partner.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerRequestServiceTest {

    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerServiceRequestRepository partnerServiceRequestRepository;
    @Mock
    private DispatchOrchestrationService dispatchOrchestrationService;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PartnerRequestService partnerRequestService;

    @Test
    void acceptShouldRejectExpiredRequestWithSlotExpired() {
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        request.setPartnerId(7L);
        request.setCartId(1001L);
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        request.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(11L, 7L)).thenReturn(Optional.of(request));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InstantBookingUnavailableException ex = assertThrows(
                InstantBookingUnavailableException.class,
                () -> partnerRequestService.accept(7L, 11L)
        );
        assertEquals(InstantUnavailableCode.SLOT_EXPIRED.name(), ex.getErrorCode());
        assertEquals(PartnerServiceRequestStatus.REJECTED, request.getStatus());
        verify(partnerServiceRequestRepository).save(request);
    }

    @Test
    void acceptShouldRejectWhenInstantOrderAlreadyAssignedToAnotherPartner() {
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        request.setPartnerId(8L);
        request.setCartId(2002L);
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        request.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        request.setEtaMinutes(10);

        Order order = new Order();
        order.setId(2002L);
        order.setBookingType("INSTANT");
        order.setStatus(OrderStatus.PARTNER_ASSIGNED);

        when(partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(12L, 8L)).thenReturn(Optional.of(request));
        when(orderRepository.findByIdForUpdate(2002L)).thenReturn(Optional.of(order));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> partnerRequestService.accept(8L, 12L));
        assertEquals("request is not pending", ex.getMessage());
        assertEquals(PartnerServiceRequestStatus.REJECTED, request.getStatus());
        assertEquals("Accepted by another partner", request.getRejectionReason());
        verify(partnerServiceRequestRepository).save(request);
    }
}
