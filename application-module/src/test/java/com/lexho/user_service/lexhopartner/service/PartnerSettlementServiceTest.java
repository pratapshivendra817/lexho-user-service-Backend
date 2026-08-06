package com.lexho.user_service.lexhopartner.service;

import com.lexho.common.enums.PaymentStatus;
import com.lexho.payment.event.PaymentRefundProcessedEvent;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.config.PartnerSettlementProperties;
import com.lexho.user_service.lexhopartner.dto.CreateWithdrawalRequest;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerSettlementRefundRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerSettlementServiceTest {

    private final PayoutRepository payoutRepository = mock(PayoutRepository.class);
    private final PartnerOrderRepository partnerOrderRepository = mock(PartnerOrderRepository.class);
    private final PartnerWithdrawalRequestRepository withdrawalRepository = mock(PartnerWithdrawalRequestRepository.class);
    private final PartnerSettlementRefundRepository refundRepository = mock(PartnerSettlementRefundRepository.class);
    private final PartnerSettlementProperties properties = new PartnerSettlementProperties();
    private final PartnerSettlementService service = new PartnerSettlementService(
            payoutRepository, partnerOrderRepository, withdrawalRepository, refundRepository, properties);

    @Test
    void ensurePayoutShouldCreateNetPayoutForCompletedSuccessfulOrderOnly() {
        properties.setCommissionPercent(new BigDecimal("10.00"));
        PartnerOrder order = partnerOrder(100L, 5L, 1000.0, OrderStatus.COMPLETED, PaymentStatus.SUCCESS);
        when(payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());
        when(partnerOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        service.ensurePayout(100L);

        verify(payoutRepository).saveAndFlush(any(Payout.class));

        PartnerOrder pending = partnerOrder(101L, 5L, 1000.0, OrderStatus.ACCEPTED, PaymentStatus.SUCCESS);
        when(partnerOrderRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pending));
        service.ensurePayout(101L);
    }

    @Test
    void ensurePayoutShouldTreatDuplicateInsertRaceAsIdempotent() {
        PartnerOrder order = partnerOrder(100L, 5L, 1000.0, OrderStatus.COMPLETED, PaymentStatus.SUCCESS);
        when(payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());
        when(partnerOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        doThrow(new DataIntegrityViolationException("duplicate payout"))
                .when(payoutRepository).saveAndFlush(any(Payout.class));

        service.ensurePayout(100L);

        verify(payoutRepository).saveAndFlush(any(Payout.class));
    }

    @Test
    void payoutsAndEarningsShouldMapPagedAndSummarizedData() {
        Payout settled = payout(1L, 5L, 100L, 1000.0, "SETTLED", LocalDateTime.now().minusDays(1));
        Payout pending = payout(2L, 5L, 101L, 500.0, "PENDING", LocalDateTime.now());
        when(payoutRepository.findByProviderId(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(settled, pending)));
        when(payoutRepository.findByIdAndProviderId(1L, 5L)).thenReturn(Optional.of(settled));
        when(payoutRepository.findByProviderId(5L)).thenReturn(List.of(settled, pending));
        when(withdrawalRepository.findByPartnerIdAndStatusIn(org.mockito.ArgumentMatchers.eq(5L), any()))
                .thenReturn(List.of(withdrawal(5L, "REQUESTED", "100.00")));

        assertEquals(2, service.payouts(5L, -1, 500).content().size());
        assertEquals(1L, service.payout(5L, 1L).id());
        assertEquals(900.0, service.earningsSummary(5L).availableBalance());
        assertThrows(ResourceNotFoundException.class, () -> service.payout(5L, 404L));
    }

    @Test
    void earningTransactionsShouldValidateDatesAndAttachOrderDetails() {
        Payout payout = payout(1L, 5L, 100L, 1000.0, "SETTLED", LocalDateTime.now());
        PartnerOrder order = partnerOrder(100L, 5L, 1000.0, OrderStatus.COMPLETED, PaymentStatus.SUCCESS);
        order.setCustomerName("Customer");
        order.setServiceType("Cleaning");
        when(payoutRepository.findByProviderIdAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(5L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payout)));
        when(partnerOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        var response = service.earningTransactions(5L, LocalDate.now().minusDays(1), LocalDate.now(), 0, 10);

        assertEquals("Customer", response.content().get(0).customerName());
        assertEquals(payout.getId(), response.content().get(0).id());
        assertEquals("EARNING", response.content().get(0).type());
        assertThrows(BadRequestException.class, () -> service.earningTransactions(
                5L, LocalDate.now(), LocalDate.now().minusDays(1), 0, 10));
    }

    @Test
    void withdrawalsShouldEnforceMinimumAndAvailableBalance() {
        properties.setMinWithdrawalAmount(new BigDecimal("100.00"));
        Payout settled = payout(1L, 5L, 100L, 1000.0, "SETTLED", LocalDateTime.now());
        when(payoutRepository.findByProviderId(5L)).thenReturn(List.of(settled));
        when(withdrawalRepository.findByPartnerIdAndStatusIn(org.mockito.ArgumentMatchers.eq(5L), any()))
                .thenReturn(List.of());
        when(withdrawalRepository.save(any(PartnerWithdrawalRequest.class))).thenAnswer(invocation -> {
            PartnerWithdrawalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 80L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });
        when(withdrawalRepository.findByPartnerId(org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withdrawal(5L, "REQUESTED", "200.00"))));

        assertEquals(80L, service.requestWithdrawal(5L, new CreateWithdrawalRequest(new BigDecimal("200.00"))).id());
        assertEquals(1, service.withdrawals(5L, 0, 10).content().size());
        assertThrows(BadRequestException.class, () -> service.requestWithdrawal(5L,
                new CreateWithdrawalRequest(new BigDecimal("50.00"))));
        assertThrows(BadRequestException.class, () -> service.requestWithdrawal(5L,
                new CreateWithdrawalRequest(new BigDecimal("2000.00"))));
    }

    @Test
    void refundEventShouldReversePayoutIdempotently() {
        Payout payout = payout(1L, 5L, 100L, 850.0, "PENDING", LocalDateTime.now());
        payout.setGrossAmount(new BigDecimal("1000.00"));
        when(refundRepository.existsById("rfnd_1")).thenReturn(false);
        when(payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(payout));

        service.onRefundProcessed(new PaymentRefundProcessedEvent(100L, "rfnd_1", new BigDecimal("500.00")));

        assertEquals(new BigDecimal("425.00"), payout.getReversedAmount());
        assertEquals("PARTIALLY_REVERSED", payout.getStatus());
        verify(refundRepository).save(any());
    }

    private Payout payout(Long id, Long partnerId, Long bookingId, Double amount, String status, LocalDateTime createdAt) {
        Payout payout = new Payout();
        ReflectionTestUtils.setField(payout, "id", id);
        ReflectionTestUtils.setField(payout, "createdAt", createdAt);
        payout.setProviderId(partnerId);
        payout.setReferenceBookingId(bookingId);
        payout.setGrossAmount(BigDecimal.valueOf(amount));
        payout.setCommissionPercent(new BigDecimal("10.00"));
        payout.setCommissionAmount(BigDecimal.valueOf(amount).multiply(new BigDecimal("0.10")));
        payout.setAmount(amount);
        payout.setCurrency("INR");
        payout.setStatus(status);
        payout.setReversedAmount(BigDecimal.ZERO);
        return payout;
    }

    private PartnerOrder partnerOrder(Long id, Long partnerId, Double amount, OrderStatus status, PaymentStatus paymentStatus) {
        PartnerOrder order = new PartnerOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setAssignedPartnerId(partnerId);
        order.setAmount(amount);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setCustomerName("Customer");
        order.setServiceType("Cleaning");
        return order;
    }

    private PartnerWithdrawalRequest withdrawal(Long partnerId, String status, String amount) {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        ReflectionTestUtils.setField(request, "id", 7L);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.now());
        request.setPartnerId(partnerId);
        request.setStatus(status);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
