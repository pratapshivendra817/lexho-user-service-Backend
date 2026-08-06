package com.lexho.user_service.partnerrequest.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.dto.PartnerRejectRequest;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusUpdateRequest;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.subscription.service.SubscriptionAutoDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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
    @Mock
    private PartnerLeaveRepository partnerLeaveRepository;
    @Mock
    private SubscriptionAutoDispatchService subscriptionAutoDispatchService;

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
        when(orderRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(new Order()));
        when(partnerServiceRequestRepository.countByCartIdAndStatus(1001L, PartnerServiceRequestStatus.PENDING)).thenReturn(0L);
        when(partnerServiceRequestRepository.countByCartIdAndStatus(1001L, PartnerServiceRequestStatus.ACCEPTED)).thenReturn(0L);
        when(dispatchOrchestrationService.retryDispatch(1001L)).thenReturn(0);

        InstantBookingUnavailableException ex = assertThrows(
                InstantBookingUnavailableException.class,
                () -> partnerRequestService.accept(7L, 11L)
        );
        assertEquals(InstantUnavailableCode.SLOT_EXPIRED.name(), ex.getErrorCode());
        assertEquals(PartnerServiceRequestStatus.EXPIRED, request.getStatus());
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

    @Test
    void updateOnlineStatusAndIncomingRequestsShouldCoverSuccessLeaveAndExpiredMapping() {
        com.lexho.provider.entity.Partner partner = new com.lexho.provider.entity.Partner();
        partner.setId(7L);
        PartnerStatusUpdateRequest update = new PartnerStatusUpdateRequest();
        update.setIsOnline(true);
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(partner)).thenReturn(partner);

        assertThat(partnerRequestService.updateOnlineStatus(7L, update).isOnline()).isTrue();

        when(partnerLeaveRepository.existsByPartnerIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(7L), any(), any())).thenReturn(true);
        assertThat(partnerRequestService.getIncomingRequests(7L, null, -1, 500).getContent()).isEmpty();

        PartnerServiceRequestEntity expired = request(20L, 7L, PartnerServiceRequestStatus.PENDING);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expired.setFullAddress("Sector 1");
        expired.setCity("Noida");
        when(partnerLeaveRepository.existsByPartnerIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(8L), any(), any())).thenReturn(false);
        when(partnerServiceRequestRepository.findByPartnerIdAndStatusOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.eq(PartnerServiceRequestStatus.PENDING),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(expired)));
        when(partnerServiceRequestRepository.save(expired)).thenReturn(expired);

        var page = partnerRequestService.getIncomingRequests(8L, PartnerServiceRequestStatus.PENDING, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(PartnerServiceRequestStatus.EXPIRED);
        assertThat(page.getContent().get(0).getAddress().getCity()).isEqualTo("Noida");
    }

    @Test
    void acceptShouldPersistAcceptedRequestRejectPeersAndDispatch() {
        PartnerServiceRequestEntity accepted = request(30L, 7L, PartnerServiceRequestStatus.PENDING);
        accepted.setCartId(300L);
        accepted.setEtaMinutes(null);
        accepted.setDistanceKm(java.math.BigDecimal.ONE);
        PartnerServiceRequestEntity peer = request(31L, 8L, PartnerServiceRequestStatus.PENDING);
        peer.setCartId(300L);
        Order order = new Order();
        order.setId(300L);
        order.setBookingType("SCHEDULED");
        when(partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(30L, 7L)).thenReturn(Optional.of(accepted));
        when(orderRepository.findByIdForUpdate(300L)).thenReturn(Optional.of(order));
        when(dispatchOrchestrationService.estimateEtaMinutes(java.math.BigDecimal.ONE)).thenReturn(12);
        when(partnerServiceRequestRepository.findByCartIdAndStatus(300L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(List.of(accepted, peer));
        when(partnerServiceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = partnerRequestService.accept(7L, 30L);

        assertThat(response.getStatus()).isEqualTo(PartnerServiceRequestStatus.ACCEPTED);
        assertThat(peer.getStatus()).isEqualTo(PartnerServiceRequestStatus.REJECTED);
        verify(dispatchOrchestrationService).markPartnerAccepted(300L, 7L, 12);
    }

    @Test
    void rejectShouldMarkInstantOrderFailedWhenNoRequestsRemain() {
        PartnerServiceRequestEntity request = request(40L, 7L, PartnerServiceRequestStatus.PENDING);
        request.setCartId(400L);
        PartnerRejectRequest rejection = new PartnerRejectRequest();
        rejection.setReason("busy");
        Order order = new Order();
        order.setId(400L);
        order.setBookingType("INSTANT");
        when(partnerServiceRequestRepository.findByIdAndPartnerId(40L, 7L)).thenReturn(Optional.of(request));
        when(orderRepository.findByIdForUpdate(400L)).thenReturn(Optional.of(order));
        when(partnerServiceRequestRepository.countByCartIdAndStatus(400L, PartnerServiceRequestStatus.PENDING)).thenReturn(0L);
        when(partnerServiceRequestRepository.countByCartIdAndStatus(400L, PartnerServiceRequestStatus.ACCEPTED)).thenReturn(0L);
        when(partnerServiceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dispatchOrchestrationService.retryDispatch(400L)).thenReturn(0);

        var response = partnerRequestService.reject(7L, 40L, rejection);

        assertThat(response.getStatus()).isEqualTo(PartnerServiceRequestStatus.REJECTED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.INSTANT_FAILED);
        verify(orderRepository).save(order);
        verify(dispatchOrchestrationService).saveEvent(org.mockito.ArgumentMatchers.eq(400L),
                org.mockito.ArgumentMatchers.eq("INSTANT_FAILED"), any());
    }

    @Test
    void detailDevSeedAndBulkExpiryShouldMapReturnPaths() {
        PartnerServiceRequestEntity detail = request(50L, 7L, PartnerServiceRequestStatus.PENDING);
        detail.setFullAddress("Address");
        when(partnerServiceRequestRepository.findByIdAndPartnerId(50L, 7L)).thenReturn(Optional.of(detail));
        assertThat(partnerRequestService.getRequestDetail(7L, 50L).getRequestId()).isEqualTo(50L);

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(partnerServiceRequestRepository.save(any())).thenAnswer(invocation -> {
            PartnerServiceRequestEntity saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 60L);
            }
            return saved;
        });
        assertThat(partnerRequestService.createDevPendingRequest(7L).getRequestId()).isEqualTo(60L);

        PartnerServiceRequestEntity expired = request(61L, 7L, PartnerServiceRequestStatus.PENDING);
        when(partnerServiceRequestRepository.findByStatusAndExpiresAtBefore(
                org.mockito.ArgumentMatchers.eq(PartnerServiceRequestStatus.PENDING), any())).thenReturn(List.of(expired));
        assertThat(partnerRequestService.expirePendingRequests()).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(PartnerServiceRequestStatus.EXPIRED);
    }

    @Test
    void acceptShouldCompleteSubscriptionRequestAndRejectPeers() {
        PartnerServiceRequestEntity accepted = request(80L, 7L, PartnerServiceRequestStatus.PENDING);
        accepted.setSubscriptionId(500L);
        accepted.setSubscriptionVisitId(600L);
        PartnerServiceRequestEntity peer = request(81L, 8L, PartnerServiceRequestStatus.PENDING);
        peer.setSubscriptionId(500L);
        peer.setSubscriptionVisitId(600L);

        when(partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(80L, 7L)).thenReturn(Optional.of(accepted));
        when(partnerServiceRequestRepository.findBySubscriptionVisitIdAndStatus(600L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(List.of(accepted, peer));
        when(partnerServiceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = partnerRequestService.accept(7L, 80L);

        assertThat(response.getStatus()).isEqualTo(PartnerServiceRequestStatus.ACCEPTED);
        assertThat(peer.getStatus()).isEqualTo(PartnerServiceRequestStatus.REJECTED);
        verify(subscriptionAutoDispatchService).markPartnerAccepted(500L, 600L, 7L);
        verify(dispatchOrchestrationService, never()).markPartnerAccepted(any(), any(), any());
    }

    @Test
    void rejectShouldRetrySubscriptionDispatchWhenNoPendingRequestsRemain() {
        PartnerServiceRequestEntity request = request(90L, 7L, PartnerServiceRequestStatus.PENDING);
        request.setSubscriptionId(501L);
        request.setSubscriptionVisitId(601L);
        PartnerRejectRequest rejection = new PartnerRejectRequest();
        rejection.setReason("busy");

        when(partnerServiceRequestRepository.findByIdAndPartnerId(90L, 7L)).thenReturn(Optional.of(request));
        when(partnerServiceRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(601L, PartnerServiceRequestStatus.PENDING)).thenReturn(0L);
        when(partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(601L, PartnerServiceRequestStatus.ACCEPTED)).thenReturn(0L);

        var response = partnerRequestService.reject(7L, 90L, rejection);

        assertThat(response.getStatus()).isEqualTo(PartnerServiceRequestStatus.REJECTED);
        verify(subscriptionAutoDispatchService).retryDispatch(501L, 601L);
        verify(dispatchOrchestrationService, never()).markPartnerRejected(any(), any());
    }

    @Test
    void missingAndInvalidStatesShouldFailWithoutDispatch() {
        when(partnerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.lexho.user_service.exception.ResourceNotFoundException.class,
                () -> partnerRequestService.updateOnlineStatus(99L, new PartnerStatusUpdateRequest()));

        PartnerServiceRequestEntity rejected = request(70L, 7L, PartnerServiceRequestStatus.REJECTED);
        when(partnerServiceRequestRepository.findByIdAndPartnerId(70L, 7L)).thenReturn(Optional.of(rejected));
        assertThrows(BadRequestException.class, () -> partnerRequestService.reject(7L, 70L, null));
        verify(dispatchOrchestrationService, never()).markPartnerRejected(70L, 7L);
    }

    private PartnerServiceRequestEntity request(Long id, Long partnerId, PartnerServiceRequestStatus status) {
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        ReflectionTestUtils.setField(request, "id", id);
        request.setPartnerId(partnerId);
        request.setCartId(100L);
        request.setCustomerId(9L);
        request.setServiceId(1L);
        request.setServiceType("Cleaning");
        request.setQuantity(1);
        request.setStatus(status);
        request.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return request;
    }
}
