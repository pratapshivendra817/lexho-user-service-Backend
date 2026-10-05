package com.lexho.user_service.accountdeletion.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.payment.repository.PaymentRefundRepository;
import com.lexho.payment.repository.PaymentTransactionRepository;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionCreateRequest;
import com.lexho.user_service.accountdeletion.dto.AccountDeletionRejectRequest;
import com.lexho.user_service.accountdeletion.entity.AccountDeletionRequest;
import com.lexho.user_service.accountdeletion.repository.AccountDeletionRequestRepository;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private final AccountDeletionRequestRepository repository = mock(AccountDeletionRequestRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final PartnerOperationalProfileRepository partnerProfileRepository = mock(PartnerOperationalProfileRepository.class);
    private final PaymentTransactionRepository paymentTransactionRepository = mock(PaymentTransactionRepository.class);
    private final PaymentRefundRepository paymentRefundRepository = mock(PaymentRefundRepository.class);
    private final PayoutRepository payoutRepository = mock(PayoutRepository.class);
    private final PartnerWithdrawalRequestRepository withdrawalRepository = mock(PartnerWithdrawalRequestRepository.class);
    private final AccountDeletionService service = new AccountDeletionService(
            repository,
            orderRepository,
            customerRepository,
            partnerProfileRepository,
            paymentTransactionRepository,
            paymentRefundRepository,
            payoutRepository,
            withdrawalRepository
    );

    @Test
    void partnerDeletionRequestShouldBlockForPayoutWithdrawalAndWithdrawableEarningObligations() {
        when(repository.existsByUserIdAndUserTypeAndStatusIn(eq(42L), eq("PARTNER"), any())).thenReturn(false);
        when(orderRepository.countByAssignedPartnerIdAndStatusIn(eq(42L), any())).thenReturn(0L);
        when(payoutRepository.countByProviderIdAndStatusIn(eq(42L), any())).thenReturn(2L);
        when(withdrawalRepository.countByPartnerIdAndStatusIn(eq(42L), any())).thenReturn(1L);
        when(payoutRepository.findByProviderId(42L)).thenReturn(List.of(payout("SETTLED", 500.00, BigDecimal.ZERO)));
        when(withdrawalRepository.findByPartnerIdAndStatusIn(eq(42L), any()))
                .thenReturn(List.of(withdrawal("PAID", "200.00")));
        when(repository.save(any())).thenAnswer(invocation -> {
            Object saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        var response = service.request(42L, "PARTNER", new AccountDeletionCreateRequest("delete account"));

        assertEquals(4L, response.pendingPartnerPayouts());
        verify(payoutRepository).countByProviderIdAndStatusIn(eq(42L),
                eq(List.of("PENDING", "PROCESSING", "PARTIALLY_REVERSED")));
        verify(withdrawalRepository).countByPartnerIdAndStatusIn(eq(42L),
                eq(List.of("REQUESTED", "APPROVED", "PROCESSING")));
    }

    @Test
    void customerDeletionRequestShouldNotRunPartnerPayoutChecks() {
        when(repository.existsByUserIdAndUserTypeAndStatusIn(eq(9L), eq("CUSTOMER"), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.request(9L, "CUSTOMER", new AccountDeletionCreateRequest(null));

        assertEquals(0L, response.pendingPartnerPayouts());
        verifyNoInteractions(payoutRepository, withdrawalRepository);
    }

    @Test
    void requestReturnsExistingOpenStatusInsteadOfCreatingDuplicate() {
        AccountDeletionRequest existing = deletionRequest(15L, 9L, "CUSTOMER", "UNDER_REVIEW");
        existing.setRequestReason("already requested");
        when(repository.existsByUserIdAndUserTypeAndStatusIn(eq(9L), eq("CUSTOMER"), any())).thenReturn(true);
        when(repository.findTopByUserIdAndUserTypeOrderByRequestedAtDesc(9L, "CUSTOMER"))
                .thenReturn(Optional.of(existing));

        var response = service.request(9L, " customer ", new AccountDeletionCreateRequest("new reason"));

        assertEquals(15L, response.id());
        assertEquals("UNDER_REVIEW", response.status());
        assertEquals("already requested", response.requestReason());
        verify(repository).findTopByUserIdAndUserTypeOrderByRequestedAtDesc(9L, "CUSTOMER");
    }

    @Test
    void statusReturnsNoneForMissingRequestAndRejectsUnsupportedRole() {
        when(repository.findTopByUserIdAndUserTypeOrderByRequestedAtDesc(19L, "PARTNER"))
                .thenReturn(Optional.empty());

        var response = service.status(19L, "partner");

        assertNull(response.id());
        assertEquals("NONE", response.status());
        assertEquals("PARTNER", response.userType());
        assertThrows(BadRequestException.class, () -> service.status(19L, "ADMIN"));
    }

    @Test
    void cancelOnlyAllowsOpenRequests() {
        AccountDeletionRequest requested = deletionRequest(21L, 9L, "CUSTOMER", "REQUESTED");
        when(repository.findTopByUserIdAndUserTypeOrderByRequestedAtDesc(9L, "CUSTOMER"))
                .thenReturn(Optional.of(requested));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("CANCELLED", service.cancel(9L, "CUSTOMER").status());

        requested.setStatus("DELETED");
        assertThrows(BadRequestException.class, () -> service.cancel(9L, "CUSTOMER"));
    }

    @Test
    void adminListTrimsAndUppercasesStatusFilter() {
        AccountDeletionRequest rejected = deletionRequest(30L, 9L, "CUSTOMER", "REJECTED");
        when(repository.findByStatusOrderByRequestedAtDesc("REJECTED")).thenReturn(List.of(rejected));

        var response = service.adminList(" rejected ");

        assertEquals(1, response.size());
        assertEquals("REJECTED", response.get(0).status());
        verify(repository).findByStatusOrderByRequestedAtDesc("REJECTED");
    }

    @Test
    void approveMovesRequestUnderReviewWhenBusinessConditionsRemain() {
        AccountDeletionRequest request = deletionRequest(31L, 9L, "CUSTOMER", "REQUESTED");
        when(repository.findById(31L)).thenReturn(Optional.of(request));
        when(orderRepository.countByUserIdAndStatusIn(eq(9L), any())).thenReturn(1L);
        when(paymentTransactionRepository.countByCustomerIdAndPaymentStatusIn(eq(9L), any())).thenReturn(1L);
        when(paymentRefundRepository.countByCustomerIdAndRefundStatusIn(eq(9L), any())).thenReturn(1L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.approve(31L, 77L));

        assertEquals("UNDER_REVIEW", request.getStatus());
        assertEquals(77L, request.getReviewedByAdminId());
        verify(repository).save(request);
    }

    @Test
    void approveSchedulesRequestWhenNoConditionsRemain() {
        AccountDeletionRequest request = deletionRequest(32L, 9L, "CUSTOMER", "UNDER_REVIEW");
        when(repository.findById(32L)).thenReturn(Optional.of(request));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.approve(32L, 77L);

        assertEquals("SCHEDULED", response.status());
        assertEquals(77L, request.getReviewedByAdminId());
    }

    @Test
    void rejectRequiresOpenRequestAndStoresTrimmedReason() {
        AccountDeletionRequest request = deletionRequest(33L, 9L, "CUSTOMER", "SCHEDULED");
        when(repository.findById(33L)).thenReturn(Optional.of(request));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reject(33L, 77L, new AccountDeletionRejectRequest("  pending refund  "));

        assertEquals("REJECTED", response.status());
        assertEquals("pending refund", response.rejectionReason());

        request.setStatus("DELETED");
        assertThrows(BadRequestException.class,
                () -> service.reject(33L, 77L, new AccountDeletionRejectRequest("no")));
    }

    @Test
    void processAnonymizesCustomerProfileAfterScheduledChecksPass() {
        AccountDeletionRequest request = deletionRequest(34L, 9L, "CUSTOMER", "SCHEDULED");
        CustomerEntity customer = new CustomerEntity();
        customer.setId(9L);
        customer.setActive(true);
        customer.setEmail("customer@example.com");
        customer.setPhoneNumber("999");
        customer.setMobile("999");
        customer.setFullName("Customer");
        customer.setProfileImageUrl("https://example.test/avatar.png");
        when(repository.findById(34L)).thenReturn(Optional.of(request));
        when(customerRepository.findById(9L)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.process(34L, 77L);

        assertEquals("DELETED", response.status());
        assertFalse(customer.getActive());
        assertNull(customer.getEmail());
        assertEquals("deleted-user-34", customer.getPhoneNumber());
        assertEquals("deleted-user-34", customer.getMobile());
        assertEquals("Deleted Customer", customer.getFullName());
        assertNull(customer.getProfileImageUrl());
        verify(customerRepository).save(customer);
    }

    @Test
    void processAnonymizesPartnerOperationalProfileAfterScheduledChecksPass() {
        AccountDeletionRequest request = deletionRequest(35L, 42L, "PARTNER", "SCHEDULED");
        PartnerOperationalProfile partner = new PartnerOperationalProfile();
        partner.setId(42L);
        partner.setPhoneNumber("888");
        partner.setFullName("Partner");
        when(repository.findById(35L)).thenReturn(Optional.of(request));
        when(partnerProfileRepository.findById(42L)).thenReturn(Optional.of(partner));
        when(payoutRepository.findByProviderId(42L)).thenReturn(List.of());
        when(withdrawalRepository.findByPartnerIdAndStatusIn(eq(42L), any())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.process(35L, 77L);

        assertEquals("DELETED", response.status());
        assertTrue(partner.isDeleted());
        assertEquals("deleted-user-35", partner.getPhoneNumber());
        assertEquals("Deleted Partner", partner.getFullName());
        verify(partnerProfileRepository).save(partner);
    }

    @Test
    void processRejectsMissingNonScheduledAndBlockedRequests() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.process(404L, 77L));

        AccountDeletionRequest request = deletionRequest(36L, 9L, "CUSTOMER", "REQUESTED");
        when(repository.findById(36L)).thenReturn(Optional.of(request));
        assertThrows(BadRequestException.class, () -> service.process(36L, 77L));

        request.setStatus("SCHEDULED");
        when(orderRepository.countByUserIdAndStatusIn(eq(9L), any())).thenReturn(2L);
        assertThrows(BadRequestException.class, () -> service.process(36L, 77L));
    }

    @Test
    void requestTrimsBlankReasonsAndCountsCustomerPaymentObligations() {
        when(repository.existsByUserIdAndUserTypeAndStatusIn(eq(11L), eq("CUSTOMER"), any())).thenReturn(false);
        when(orderRepository.countByUserIdAndStatusIn(eq(11L), any())).thenReturn(2L);
        when(paymentTransactionRepository.countByCustomerIdAndPaymentStatusIn(eq(11L), any())).thenReturn(3L);
        when(paymentRefundRepository.countByCustomerIdAndRefundStatusIn(eq(11L), any())).thenReturn(4L);
        when(repository.save(any())).thenAnswer(invocation -> {
            AccountDeletionRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 41L);
            return saved;
        });

        var response = service.request(11L, "CUSTOMER", new AccountDeletionCreateRequest("   "));

        assertNull(response.requestReason());
        assertEquals(2L, response.activeBookings());
        assertEquals(3L, response.pendingPayments());
        assertEquals(4L, response.pendingRefunds());
    }

    private AccountDeletionRequest deletionRequest(Long id, Long userId, String userType, String status) {
        AccountDeletionRequest request = new AccountDeletionRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setUserId(userId);
        request.setUserType(userType);
        request.setStatus(status);
        return request;
    }

    private Payout payout(String status, Double amount, BigDecimal reversedAmount) {
        Payout payout = new Payout();
        payout.setProviderId(42L);
        payout.setAmount(amount);
        payout.setReversedAmount(reversedAmount);
        payout.setStatus(status);
        return payout;
    }

    private PartnerWithdrawalRequest withdrawal(String status, String amount) {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setPartnerId(42L);
        request.setStatus(status);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
